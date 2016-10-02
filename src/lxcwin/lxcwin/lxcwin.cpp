#include "stdafx.h"
#include "lxcwin.h"


extern "C" LXCWIN_API void* allocTaskbarObject() {
	CoInitialize(nullptr);
	ITaskbarList3* m_taskbarInterface = (ITaskbarList3*) malloc(sizeof(ITaskbarList3*));
	HRESULT hr = CoCreateInstance(CLSID_TaskbarList, NULL, CLSCTX_INPROC_SERVER, IID_ITaskbarList3, reinterpret_cast<void**> (&(m_taskbarInterface)));
	if (SUCCEEDED(hr)) {
		hr = m_taskbarInterface->HrInit();
		if (SUCCEEDED(hr)) {
			return m_taskbarInterface;
		}
	}
	return NULL;
}

extern "C" LXCWIN_API BOOL setProgressValue(void* taskbar, HWND hwnd, int percent) {
	if (taskbar == NULL) {
		return false;
	}
	if (percent < 0 || percent > 100) {
		return false;
	}
	HRESULT hr = ((ITaskbarList3*)taskbar)->SetProgressValue(hwnd, percent, 100);
	return SUCCEEDED(hr);
}

extern "C" LXCWIN_API BOOL setProgressState(void* taskbar, HWND hwnd, TBPFLAG state) {
	if (taskbar == NULL) {
		return false;
	}
	HRESULT hr = ((ITaskbarList3*)taskbar)->SetProgressState(hwnd, state);
	return SUCCEEDED(hr);
}
