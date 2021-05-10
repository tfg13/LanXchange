use std::{env, thread};
use std::fs::{self, OpenOptions, File};
use std::path::Path;
use std::ffi::OsString;
use std::process::{Command, Stdio};
use std::time::{Instant, Duration};
use std::io::Write;

const UPDATE_DIR_NAME: &str = "tmp_update";
const MAIN_EXE: &str = "lxc.exe";

const SLEEP_MS: Duration = Duration::from_millis(500);
const MAX_TOTAL_SLEEP: Duration = Duration::from_secs(30);

struct Updater {
    log: std::fs::File,
    marked_err: bool,
}

fn main() {
    let args: Vec<String> = env::args().collect();

    match args.len() {
        2 => {
            match &args[1][..] {
                "-managed" => managed(),
                "-detach" => detach(&args[0][..]),
                _ => usage(),
            }
        },
        _ => {
            usage();
        }
    }
}

fn detach(runas: &str) {
    // JVM might not spawn a proper process and instead stop the child process during system.exit
    // Jump immediately into a new process to detach from the parent
    match Command::new(runas).arg("-managed").stdin(Stdio::null()).stdout(Stdio::null()).stderr(Stdio::null()).spawn() {
        Err(e) => {
            eprintln!("unable to run main exe: {}", e);
            std::process::exit(1);
        }
        Ok(..) => {
            // throw away the child handle on purpose
        }
    };
    std::process::exit(0);
}

fn managed() {
    let mut updater = Updater {
        log: match OpenOptions::new().create(true).write(true).truncate(true).open("update_helper.log") {
            Err(e) => {
                eprintln!("unable to open logfile for writing: {}", e);
                std::process::exit(1);
            }
            Ok(file) => file
        },
        marked_err: false,
    };

    updater.update();
}

fn usage() {
    eprintln!("this is a helper for the updater and should not be run by hand");
    std::process::exit(1);
}

impl Updater {
    // best effort write to logfile
    // always write to stderr as well
    fn log(&mut self, msg: &str) {
        match self.log.write_all(msg.as_bytes()) {
            _ => {}
        }
        match self.log.write_all(b"\n") {
            _ => {}
        }
        match self.log.sync_all() {
            _ => {}
        }
        eprintln!("{}", msg)
    }

    // main update logic
    fn update(&mut self) {
        self.log("checking environment");
        if !self.env_check() {
            self.mark_error();
            return
        }
        self.log("waiting for main exe to stop (become writable)");
        if !self.wait_exe() {
            self.mark_error();
            return
        }
        self.log("copying update files");
        self.walk_copy(Path::new(UPDATE_DIR_NAME), Path::new(""));
        self.log("restarting main exe");
        self.run_lanxchange();
        self.log("update helper done");
    }

    fn mark_error(&mut self) {
        if !self.marked_err {
            match File::create(Path::new("UPDATE_FAILED_MARKER")) {
                Ok(..) => {
                    self.marked_err = true
                }
                Err(..) => {}
            }
        }
    }

    // Check the update environment is ok.
    // This updater always overwrites the main .exe,
    // so only run if there is a new one in the update dir.
    fn env_check(&mut self) -> bool {
        match File::open(Path::new(UPDATE_DIR_NAME).join(MAIN_EXE)) {
            Err(..) => {
                self.log("refusing update, missing update main exe");
                return false
            },
            Ok(..) => {},
        }
        return true
    }

    // wait for main .exe to become writable
    fn wait_exe(&mut self) -> bool {
        let sleep_start = Instant::now();
        loop {
            if sleep_start.elapsed() > MAX_TOTAL_SLEEP {
                self.log("timeout waiting for exe to stop");
                return false
            }
            // probe with open for writing (truncate)
            let exe = OpenOptions::new().write(true).truncate(true).open("lxc.exe");
            match exe {
                Ok(..) => return true,
                Err(e) => {
                    match e.raw_os_error() {
                        // 32 is used by another process, 33 is other process has locked file
                        Some(32) | Some(33) => {
                            self.log(&format!("exe still running... ({})", e));
                            thread::sleep(SLEEP_MS);
                        }
                        Some(..) | None => {
                            // no sense waiting here
                            self.log(&format!("unable to truncate exe, but doesn't seem busy: {}", e));
                            return false
                        }
                    }
                }
            }
        }
    }


    // recursive copy operation
    fn walk_copy(&mut self, dir: &Path, prefix: &Path) {
        let dir_iter = match fs::read_dir(dir) {
            Ok(iter) => iter,
            Err(e) => {
                self.log(&format!("Unable to read dir {}: {}", dir.display(), e));
                self.mark_error();
                return
            }
        };
        for entry in dir_iter {
            let entry = match entry {
                Ok(entry) => entry,
                Err(e) => {
                    self.log(&format!("Unable examine dir {} entry: {}", dir.display(), e));
                    self.mark_error();
                    continue
                }
            };
            let path = entry.path();
            if path.is_dir() {
                // recurse
                self.walk_copy(&path, &prefix.join(entry.file_name()));
            } else {
                match self.copy_file(&path, &entry.file_name(), prefix) {
                    Err(e) => {
                        self.log(&format!("Unable to copy file {}: {}", path.display(), e));
                        self.mark_error();
                        continue
                    }
                    Ok(..) => {}
                }
                // remove if copy worked, this is best effort
                match fs::remove_file(&path) {
                    Err(e) => {
                        self.log(&format!("Unable to remove file {}: {}", path.display(), e));
                        self.mark_error();
                    }
                    Ok(..) => {}
                }
            }
        }
        // remove dir. fails if any files inside could not be moved/deleted
        match fs::remove_dir(&dir) {
            Err(e) => {
                self.log(&format!("Unable to remove dir {}: {}", dir.display(), e));
                self.mark_error();
            }
            Ok(..) => {}
        }
    }

    // copy file helper
    fn copy_file(&mut self, dir: &Path, name: &OsString, prefix: &Path) -> std::io::Result<()> {
        let target = prefix.join(name);
        self.log(&format!("copying {}, prefix {}, target {}", dir.display(), prefix.display(), target.display()));
        fs::create_dir_all(prefix)?;
        return fs::copy(dir, target).map(|_| {});
    }

    fn run_lanxchange(&mut self) {
        match Command::new(MAIN_EXE).stdin(Stdio::null()).stdout(Stdio::null()).stderr(Stdio::null()).spawn() {
            Err(e) => {
                self.log(&format!("unable to run main exe: {}", e));
                self.mark_error();
            }
            Ok(..) => {
                // throw away the child handle on purpose,
                // updater dies before the child
            }
        };
    }
}