#ifdef LXCWIN_EXPORTS
#define LXCWIN_API __declspec(dllexport)
#else
#define LXCWIN_API __declspec(dllimport)
#endif

// does nothing, called to test the native library works
extern "C" LXCWIN_API void nop();

// alloc a ITaskbarList3 object (heap), return pointer
extern "C" LXCWIN_API void* allocTaskbarObject();

// use the given taskbar object to set progress of the window with the given handle
extern "C" LXCWIN_API BOOL setProgressValue(void* taskbar, HWND hwnd, int percent);

// use the given taskbar object to change the state of the window with the given handle
extern "C" LXCWIN_API BOOL setProgressState(void* taskbar, HWND hwnd, TBPFLAG state);

// display an "open file" dialog, return the selected files
extern "C" LXCWIN_API HRESULT fileOpenDialog(HWND hwnd, DWORD *count, LPWSTR** result);

// display a "save in dir" dialog with the given title, return the target path
extern "C" LXCWIN_API HRESULT fileSaveDialog(HWND hwnd, LPWSTR dialogTitle, LPWSTR* result);

// free the strings returned by fileOpenDialog
extern "C" LXCWIN_API void cleanupOpenDialogResults(DWORD count, LPWSTR *paths);

// free the string returned by fileCloseDialog
extern "C" LXCWIN_API void cleanupSaveDialogResult(LPWSTR path);