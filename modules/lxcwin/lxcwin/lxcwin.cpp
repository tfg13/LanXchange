#include "stdafx.h"
#include "lxcwin.h"
#include <strsafe.h>
#include "Knownfolders.h"

extern "C" LXCWIN_API void nop() {
	// do nothing, this is called to verify native calls work
	return;
}

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

extern "C" LXCWIN_API HRESULT fileOpenDialog(HWND hWnd, DWORD *count, LPWSTR **result) {
	*result = NULL;
	HRESULT hr = S_OK;
	CoInitialize(nullptr);
	IFileOpenDialog *pfd = NULL;
	hr = CoCreateInstance(CLSID_FileOpenDialog, NULL, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&pfd));
	if (SUCCEEDED(hr)) {
		// set default folder to "My Documents"
		IShellItem *psiDocuments = NULL;
		hr = SHCreateItemInKnownFolder(FOLDERID_Documents, 0, NULL,
			IID_PPV_ARGS(&psiDocuments));
		if (SUCCEEDED(hr)) {
			hr = pfd->SetDefaultFolder(psiDocuments);
			psiDocuments->Release();
		}

		// dialog title
		pfd->SetTitle(L"Select files to share");

		// allow multiselect, restrict to real files
		DWORD dwOptions;
		hr = pfd->GetOptions(&dwOptions);
		if (SUCCEEDED(hr)) {
			// ideally, allow selecting folders as well as files, but IFileDialog does not support this :(
			pfd->SetOptions(dwOptions | FOS_ALLOWMULTISELECT | FOS_FORCEFILESYSTEM); // | FOS_PICKFOLDERS
		}

		// do not limit to certain file types

		// show the open file dialog
		hr = pfd->Show(hWnd);
		if (SUCCEEDED(hr)) {
			IShellItemArray *psiaResults;
			hr = pfd->GetResults(&psiaResults);
			if (SUCCEEDED(hr)) {
				hr = psiaResults->GetCount(count);
				if (SUCCEEDED(hr)) {
					*result = (LPWSTR*)calloc(*count, sizeof(LPWSTR));
					if (*result != NULL) {
						for (DWORD i = 0; i < *count; i++) {
							IShellItem *resultItem = NULL;
							hr = psiaResults->GetItemAt(i, &resultItem);
							if (SUCCEEDED(hr)) {
								resultItem->GetDisplayName(SIGDN_FILESYSPATH, &((*result)[i]));
								resultItem->Release();
							}
						}
						// paths now contains selected files
					}
				}
				psiaResults->Release();
			}
		}
		pfd->Release();
	}
	return hr;
}

extern "C" LXCWIN_API HRESULT fileSaveDialog(HWND hWnd, LPWSTR dialogTitle, LPWSTR *result) {
	*result = NULL;
	HRESULT hr = S_OK;
	CoInitialize(nullptr);
	IFileOpenDialog *pfd = NULL;// yes, *open*, since this dialog selects an existing parent dir, not a new file
	hr = CoCreateInstance(CLSID_FileOpenDialog, NULL, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&pfd));
	if (SUCCEEDED(hr)) {
		// set default folder to "My Documents"
		IShellItem *psiDocuments = NULL;
		hr = SHCreateItemInKnownFolder(FOLDERID_Documents, 0, NULL,
			IID_PPV_ARGS(&psiDocuments));
		if (SUCCEEDED(hr)) {
			hr = pfd->SetDefaultFolder(psiDocuments);
			psiDocuments->Release();
		}

		// dialog title
		pfd->SetTitle(dialogTitle);

		// ok button text
		pfd->SetOkButtonLabel(L"Choose target");

		// only choose directories
		DWORD dwOptions;
		hr = pfd->GetOptions(&dwOptions);
		if (SUCCEEDED(hr)) {
			pfd->SetOptions(dwOptions | FOS_PICKFOLDERS);
		}

		// show the save dialog
		hr = pfd->Show(hWnd);
		if (SUCCEEDED(hr)) {
			IShellItem *psiaResult;
			hr = pfd->GetResult(&psiaResult);
			if (SUCCEEDED(hr)) {
				psiaResult->GetDisplayName(SIGDN_FILESYSPATH, &(*result));
				psiaResult->Release();
			}
		}
		pfd->Release();
	}
	return hr;
}

extern "C" LXCWIN_API void cleanupOpenDialogResults(DWORD count, LPWSTR *paths) {
	for (DWORD i = 0; i < count; i++) {
		CoTaskMemFree(paths[i]);
	}
}

extern "C" LXCWIN_API void cleanupSaveDialogResult(LPWSTR path) {
	CoTaskMemFree(path);
}
