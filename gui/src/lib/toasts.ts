import type { ToastSettings } from "@skeletonlabs/skeleton";

export const loginErrToast: ToastSettings = {
    message: 'Error during login',
    background: 'variant-filled-warning',
    hideDismiss: true,
    timeout: 2000
}

export const loginSuccToast: ToastSettings = {
    message: 'Successfully logged in',
    hideDismiss: true,
    timeout: 1000
}

export const loginCacheFail: ToastSettings = {
    message: 'Failed to login with saved data',
    background: 'variant-filled-warning',
    hideDismiss: true,
    timeout: 2000
}