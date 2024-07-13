<script lang="ts">
    import '../app.postcss';

    import { Child, Command } from '@tauri-apps/api/shell';
    import { resolveResource } from '@tauri-apps/api/path';
    import { fetch, ResponseType } from '@tauri-apps/api/http';
    import { getVersion } from '@tauri-apps/api/app';
    import {
        getModalStore,
        getToastStore,
        initializeStores,
        ListBox,
        ListBoxItem,
        Modal,
        Toast,
        AppShell,
        type ModalComponent,
    } from '@skeletonlabs/skeleton';

    import LoginModal from '$lib/loginModal.svelte';
    import { loginModalSettings } from '$lib/modals';
    import { tryHandshake } from '$lib/login';
    import { loginCacheFail, loginSuccToast } from '$lib/toasts';
    import {
        cacheMetaData,
        getMetaData,
        type MetaData,
        type MetaLibrary,
    } from '$lib/metadata';

    import { metaLibStore } from '$lib/stores';
    import { onDestroy } from 'svelte';
    import Icon from '@iconify/svelte';

    initializeStores();

    let clientProc: Child | null = null;
    let selected: MetaData | null = null;

    let appVersion: string = 'undefined';

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

    async function updateMeta() {
        await cacheMetaData();
        if (getMetaData() != null) {
            //@ts-ignore
            metaLibStore.set(getMetaData());
            //@ts-ignore
            if (selected == null) getMetaData()[0];
        }
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
            toastStore.trigger(loginCacheFail);
            while (sessionStorage.getItem('logged-in') == 'false') {
                if (sessionStorage.getItem('login-alive') == 'false') {
                    modalStore.trigger(loginModalSettings);
                }
                await new Promise((r) => setTimeout(r, 100));
            }
        } else {
            toastStore.trigger(loginSuccToast);
        }

        const updateInt = setInterval(updateMeta, 10000);
        onDestroy(() => {
            clearInterval(updateInt);
        });
        updateMeta();
    }

    async function setVer() {
        appVersion = await getVersion();
    }

    setVer();
    startClient();
    const kaInt = setInterval(keepAliveReq, 1000);
    onDestroy(() => {
        clearInterval(kaInt);
    });

    let metaLib: MetaLibrary;
    metaLibStore.subscribe((value) => {
        metaLib = value;
    });
</script>

<Toast />
<Modal components={modalRegistry} />

<AppShell>
    <svelte:fragment slot="sidebarLeft">
        <div class="h-screen">
            <div class="card p-4 h-full min-w-64 max-w-64 overflow-y-auto">
                <h2 class="h2">Library</h2>
                <hr class="!border-t-4 my-2" />
                <ListBox>
                    {#each metaLib as app}
                        <ListBoxItem
                            bind:group={selected}
                            name="medium"
                            value={app.name}
                        >
                            <div class="flex align-middle max-w-fit">
                                <div class="py-1">
                                    <Icon icon="mdi:application" />
                                </div>
                                <p class="px-4 truncate text-center">
                                    {app.name}
                                </p>
                            </div>
                        </ListBoxItem>
                    {/each}
                </ListBox>
            </div>
        </div>
    </svelte:fragment>
    <svelte:fragment slot="pageFooter">
        <p class="text-right text-slate-600">
            Gooselib v{appVersion}
        </p>
    </svelte:fragment>
    <slot />
</AppShell>
