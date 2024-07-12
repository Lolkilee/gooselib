<script lang="ts">
    import '../app.postcss';

    import { Child, Command } from '@tauri-apps/api/shell';
    import { resolveResource } from '@tauri-apps/api/path';
    import { fetch, ResponseType } from '@tauri-apps/api/http';
    import {
        getModalStore,
        getToastStore,
        initializeStores,
        Modal,
        Toast,
        type ModalComponent,
    } from '@skeletonlabs/skeleton';

    import LoginModal from '$lib/loginModal.svelte';
    import { loginModalSettings } from '$lib/modals';
    import { tryHandshake } from '$lib/login';
    import { loginSuccToast } from '$lib/toasts';

    initializeStores();

    let clientProc: Child | null = null;

    const BASE_URL = 'http://localhost:7123/';
    const modalStore = getModalStore();
    const toastStore = getToastStore();
    const modalRegistry: Record<string, ModalComponent> = {
        loginModal: { ref: LoginModal },
    };

    async function pingClient(): Promise<boolean> {
        try {
            const resp = await fetch(BASE_URL, {
                method: 'GET',
                responseType: ResponseType.Text,
            });
            if (resp.ok) return true;
            else return false;
        } catch (err) {
            console.log(err);
            return false;
        }
    }

    async function keepAliveReq() {
        await pingClient();
    }

    async function startClient() {
        sessionStorage.setItem('logged-in', 'false');
        sessionStorage.setItem('login-alive', 'false');

        const clientAlive = await pingClient();
        if (!clientAlive) {
            let resourcePath: string = await resolveResource('java/');
            resourcePath = resourcePath.replace('\\\\?\\', '');
            console.log('starting jar from: ' + resourcePath);
            const comm = Command.sidecar('../binaries/exec_client', [
                resourcePath,
            ]);
            clientProc = await comm.spawn();
        }

        await new Promise((r) => setTimeout(r, 1000));

        let attempt = false;
        const savUser = localStorage.getItem('username');
        const savPass = localStorage.getItem('password');
        const ip = localStorage.getItem('ip');

        if (savUser && savPass && ip) {
            attempt = await tryHandshake(savUser, savPass, ip);
        }

        if (!attempt) {
            while (sessionStorage.getItem('logged-in') == 'false') {
                if (sessionStorage.getItem('login-alive') == 'false') {
                    modalStore.trigger(loginModalSettings);
                }
                await new Promise((r) => setTimeout(r, 100));
            }
        } else {
            toastStore.trigger(loginSuccToast);
        }
    }

    startClient();
    setInterval(keepAliveReq, 1000);
</script>

<Toast />
<Modal components={modalRegistry} />

<slot />
