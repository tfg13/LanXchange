#ifdef LXCWIN_EXPORTS
#define LXCWIN_API __declspec(dllexport)
#else
#define LXCWIN_API __declspec(dllimport)
#endif

#include "Shobjidl.h"

// alloc a ITaskbarList3 object (heap), return pointer
extern "C" LXCWIN_API void* allocTaskbarObject();

// use the given taskbar object to set progress of the window with the given handle
extern "C" LXCWIN_API BOOL setProgressValue(void* taskbar, HWND hwnd, int percent);

// use the given taskbar object to change the state of the window with the given handle
extern "C" LXCWIN_API BOOL setProgressState(void* taskbar, HWND hwnd, TBPFLAG state);
