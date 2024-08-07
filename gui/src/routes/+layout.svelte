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
        ProgressRadial,
        Drawer,
        type DrawerSettings,
        getDrawerStore,
    } from '@skeletonlabs/skeleton';

    import LoginModal from '$lib/LoginModal.svelte';
    import { loginModalSettings } from '$lib/modals';
    import { tryHandshake } from '$lib/login';
    import {
        downloadCompeleteToast,
        loginCacheFail,
        loginSuccToast,
        logoutToast,
        svrLostToast,
    } from '$lib/toasts';
    import {
        cacheMetaData,
        getMetaData,
        type MetaData,
        type MetaLibrary,
    } from '$lib/metadata';

    import {
        consoleLineStore,
        downloadInfoStore,
        javaLineStore,
        metaLibStore,
        selectedMetaStore,
    } from '$lib/stores';
    import { onDestroy, onMount } from 'svelte';
    import Icon from '@iconify/svelte';
    import { goto } from '$app/navigation';
    import DownloadDrawer from '$lib/DownloadDrawer.svelte';
    import { getDownloadInfos } from '$lib/download';

    initializeStores();

    const storedTheme = localStorage.getItem('theme');
    if (storedTheme != null) document.body.dataset.theme = storedTheme;

    const storedInstallFolder = localStorage.getItem('installFolder');
    if (!storedInstallFolder) localStorage.setItem('installFolder', './');

    let clientProc: Child | null = null;
    let selected: MetaData | null = null;

    let appVersion: string = 'undefined';

    let lastDownloadCount = 0;

    const BASE_URL = 'http://localhost:7123/';
    const modalStore = getModalStore();
    const toastStore = getToastStore();
    const drawerStore = getDrawerStore();

    const modalRegistry: Record<string, ModalComponent> = {
        loginModal: { ref: LoginModal },
    };

    let starting = true;

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
        if (
            getMetaData() != null &&
            !(sessionStorage.getItem('login-alive') == 'true')
        ) {
            //@ts-ignore
            metaLibStore.set(getMetaData());
        }
    }

    async function connectedUpdate() {
        const resp = await fetch(BASE_URL + 'connection');
        //@ts-ignore
        const alive = resp.data?.connected;
        if (
            !alive &&
            alive != null &&
            !(sessionStorage.getItem('login-alive') == 'true') &&
            !starting
        ) {
            toastStore.trigger(svrLostToast);
            logout(false);
        }
    }

    async function updateDownloadInfo() {
        const newInfo = await getDownloadInfos();
        sessionStorage.setItem('download-info', JSON.stringify(newInfo));
        downloadInfoStore.set(newInfo);
    }

    downloadInfoStore.subscribe((val) => {
        if (val.length < lastDownloadCount)
            toastStore.trigger(downloadCompeleteToast);
        lastDownloadCount = val.length;
    });

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

            comm.stdout.on('data', (line) => {
                if (line != '') {
                    line.replace('\n', '');
                    javaLineStore.update((arr) => [...arr, line]);
                }
                console.log(line);
            });
            comm.stderr.on('data', (line) => {
                if (line != '') {
                    line.replace('\n', '');
                    javaLineStore.update((arr) => [...arr, line]);
                }
                console.log(line);
            });

            clientProc = await comm.spawn();
            await new Promise((r) => setTimeout(r, 1000));
        }

        if (sessionStorage.getItem('logged-in') == 'false') {
            await loginProcess();
        }

        starting = false;
        updateMeta();
    }

    async function loginProcess(triggerCacheErr: boolean = true) {
        let attempt = false;
        const savUser = localStorage.getItem('username');
        const savPass = localStorage.getItem('password');
        const ip = localStorage.getItem('ip');

        if (savUser && savPass && ip) {
            attempt = await tryHandshake(savUser, savPass, ip);
        }

        if (!attempt) {
            if (triggerCacheErr) toastStore.trigger(loginCacheFail);
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

    async function setVer() {
        appVersion = await getVersion();
    }

    async function logout(resetPass = true) {
        if (sessionStorage.getItem('login-alive') == 'false') {
            if (resetPass) {
                localStorage.removeItem('password');
                toastStore.trigger(logoutToast);
            }
            sessionStorage.setItem('logged-in', 'false');
            sessionStorage.setItem('login-alive', 'false');
            metaLibStore.set([]);
            loginProcess(false);
        }
    }

    setVer();
    startClient();

    const kaInt = setInterval(keepAliveReq, 1000);
    const udiInt = setInterval(updateDownloadInfo, 100);
    const updateInt = setInterval(updateMeta, 1000);
    const aliveInt = setInterval(connectedUpdate, 5000);
    onDestroy(() => {
        clearInterval(kaInt);
        clearInterval(udiInt);
        clearInterval(updateInt);
        clearInterval(aliveInt);
    });

    let metaLib: MetaLibrary | null = null;
    metaLibStore.subscribe((value) => {
        metaLib = value;
    });

    function navigateToPage(appName: string) {
        const route = `/app/${appName}`;
        goto(route);
    }

    async function navigateToExtPage(path: string) {
        selected = null;
        try {
            await goto(path);
        } catch (err) {
            console.log(err);
        }
    }

    function openDownloadDrawer() {
        const sett: DrawerSettings = {
            id: 'download-drawer',
        };
        drawerStore.open(sett);
    }

    function closeDownloadDrawer() {
        drawerStore.close();
    }

    selectedMetaStore.subscribe((value) => {
        selected = value;
        console.log(selected);
    });
</script>

<Toast />
<Modal components={modalRegistry} />

<Drawer>
    {#if $drawerStore.id === 'download-drawer'}
        <div class="flex flex-col justify-between h-screen">
            <DownloadDrawer></DownloadDrawer>
            <div class="flex justify-between">
                <div></div>
                <button
                    type="button"
                    class="btn variant-filled-error w-20"
                    on:click={closeDownloadDrawer}
                >
                    <Icon icon="lucide:arrow-left-to-line"></Icon>
                </button>
            </div>
        </div>
    {/if}
</Drawer>

<div class="h-screen w-screen">
    <AppShell>
        <svelte:fragment slot="sidebarLeft">
            <div
                class="card p-4 h-screen min-w-64 max-w-64 overflow-y-auto flex flex-col justify-between"
            >
                <div>
                    <button
                        class="flex"
                        on:click={() => {
                            selected = null;
                            goto('/');
                        }}
                    >
                        <Icon
                            icon="mdi:books"
                            class="mr-4"
                            width="36"
                            height="36"
                        />
                        <h2 class="h2">Library</h2>
                    </button>
                    <hr class="!border-t-4 my-2" />
                    {#if metaLib != null}
                        <ListBox>
                            {#each metaLib as app}
                                <ListBoxItem
                                    bind:group={selected}
                                    class="card my-2"
                                    name="medium"
                                    value={app}
                                    on:click={() => {
                                        navigateToPage(app.name);
                                    }}
                                >
                                    <div class="flex align-middle max-w-fit">
                                        <div class="py-1">
                                            <Icon icon="mdi:application" />
                                        </div>
                                        <p class="px-4 truncate text-center">
                                            {app.name.replaceAll('_', ' ')}
                                        </p>
                                    </div>
                                </ListBoxItem>
                            {/each}
                        </ListBox>
                    {:else}
                        <div class="placeholder animate-pulse"></div>
                    {/if}
                </div>
            </div>
        </svelte:fragment>
        <div class="card m-6 p-6 h-full max-h-[87vh] variant-soft">
            <slot />
        </div>
        <svelte:fragment slot="pageFooter">
            <div class="flex justify-between">
                <div class="mx-6 mb-1">
                    <button
                        type="button"
                        class="btn variant-filled"
                        on:click={() => {
                            selected = null;
                            goto('/settings');
                        }}
                    >
                        <Icon icon="mdi:settings" />
                    </button>
                    <button
                        type="button"
                        class="btn variant-filled"
                        on:click={openDownloadDrawer}
                    >
                        <Icon icon="mdi:chart-line" />
                    </button>
                    <button
                        type="button"
                        class="btn variant-filled"
                        on:click={async () => {
                            await navigateToExtPage('/console');
                        }}
                    >
                        <Icon icon="material-symbols:terminal-sharp" />
                    </button>
                    <button
                        type="button"
                        class="btn variant-filled-error"
                        on:click={() => {
                            logout();
                        }}
                    >
                        <Icon icon="material-symbols:logout" />
                    </button>
                </div>
                <p class="mt-2 mr-3 text-right text-slate-600">
                    Gooselib v{appVersion}
                </p>
            </div>
        </svelte:fragment>
    </AppShell>
</div>
