// Note: Modified by Tobias Fleig to support restarting on exitcode 6.
/*
	Launch4j (http://launch4j.sourceforge.net/)
	Cross-platform Java application wrapper for creating Windows native executables.

	Copyright (c) 2004, 2007 Grzegorz Kowal
							 Sylvain Mina (single instance patch)

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	Except as contained in this notice, the name(s) of the above copyright holders
	shall not be used in advertising or otherwise to promote the sale, use or other
	dealings in this Software without prior written authorization.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.
*/

#include "../resource.h"
#include "../head.h"
#include "guihead.h"

extern FILE* hLog;
extern PROCESS_INFORMATION pi;

HWND hWnd;
DWORD dwExitCode = 0;
BOOL stayAlive = FALSE;
BOOL splash = FALSE;
BOOL splashTimeoutErr;
BOOL waitForWindow;
int splashTimeout = DEFAULT_SPLASH_TIMEOUT;

int APIENTRY WinMain(HINSTANCE hInstance,
                     HINSTANCE hPrevInstance,
                     LPSTR     lpCmdLine,
                     int       nCmdShow) {
	int retCode = 6;
	while (retCode == 6) {
		int result = prepare(lpCmdLine);
		if (result == ERROR_ALREADY_EXISTS) {
			HWND handle = getInstanceWindow();
			ShowWindow(handle, SW_SHOW);
			SetForegroundWindow(handle);
			closeLogFile();
			return 2;
		}
		if (result != TRUE) {
			signalError();
			return 1;
		}

		retCode = execute(TRUE);
//		if (retCode == -1) {
//			signalError();
//			return 1;
//		}
//		if (!(splash || stayAlive)) {
//			debug("Exit code:\t0\n");
//			closeHandles();
//			return 0;
//		}
//
//		MSG msg;
//		while (GetMessage(&msg, NULL, 0, 0)) {
//			TranslateMessage(&msg);
//			DispatchMessage(&msg);
//		}
//		debug("Exit code:\t%d\n", dwExitCode);
//		closeHandles();
//		return dwExitCode;
	}
	debug("Exit code:\t0\n");
	closeHandles();
	return retCode;
}

HWND getInstanceWindow() {
	char windowTitle[STR];
	char instWindowTitle[STR] = {0};
	if (loadString(INSTANCE_WINDOW_TITLE, instWindowTitle)) {
		HWND handle = FindWindowEx(NULL, NULL, NULL, NULL); 
		while (handle != NULL) {
			GetWindowText(handle, windowTitle, STR - 1);
			if (strstr(windowTitle, instWindowTitle) != NULL) {
				return handle;
			} else {
				handle = FindWindowEx(NULL, handle, NULL, NULL);
			}
		}
	}
	return NULL;   
}

BOOL CALLBACK enumwndfn(HWND hwnd, LPARAM lParam) {
	DWORD processId;
	GetWindowThreadProcessId(hwnd, &processId);
	if (pi.dwProcessId == processId) {
		LONG styles = GetWindowLong(hwnd, GWL_STYLE);
		if ((styles & WS_VISIBLE) != 0) {
			splash = FALSE;
			ShowWindow(hWnd, SW_HIDE);
			return FALSE;
		}
	}
	return TRUE;
}

VOID CALLBACK TimerProc(
	HWND hwnd,			// handle of window for timer messages
	UINT uMsg,			// WM_TIMER message
	UINT idEvent,		// timer identifier
	DWORD dwTime) {		// current system time
	
	if (splash) {
		if (splashTimeout == 0) {
			splash = FALSE;
			ShowWindow(hWnd, SW_HIDE);
			if (waitForWindow && splashTimeoutErr) {
				KillTimer(hwnd, ID_TIMER);
				signalError();
				PostQuitMessage(0);
			}
		} else {
			splashTimeout--;
			if (waitForWindow) {
				EnumWindows(enumwndfn, 0);
			}
		}
	}
	GetExitCodeProcess(pi.hProcess, &dwExitCode);
	if (dwExitCode != STILL_ACTIVE
			|| !(splash || stayAlive)) {
		KillTimer(hWnd, ID_TIMER);
		PostQuitMessage(0);
	}
}
