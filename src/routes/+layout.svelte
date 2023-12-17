<script lang="ts">
    import "@skeletonlabs/skeleton/themes/theme-crimson.css";
    import "@skeletonlabs/skeleton/styles/skeleton.css";
    import "../app.postcss";
    import Icon from "@iconify/svelte";
    import { page } from "$app/stores";
    import { getVersion } from "@tauri-apps/api/app";
    import { ResponseType, getClient } from "@tauri-apps/api/http";
    import {
        AppShell,
        AppRail,
        AppRailAnchor,
        toastStore,
        type ToastSettings,
        Toast,
        Modal,
    } from "@skeletonlabs/skeleton";

    let appVersion: string = "undefined";

    export let lib: Library = { apps: [] };

    async function onStart() {
        appVersion = await getVersion();
    }

    async function onRefreshClick() {
        if (localStorage.getItem("saved-address") != null) {
            try {
                const updtMsg: ToastSettings = {
                    message: "Updating library...",
                    hideDismiss: true,
                    timeout: 5000,
                    background: "variant-filled-primary",
                };
                const tId = toastStore.trigger(updtMsg);
                const client = await getClient();
                const address = localStorage.getItem("saved-address");
                const reqHeaders: Record<string, any> = {
                    pw: localStorage.getItem("server-password"),
                };
                const response = await client.get<Library>(
                    "http://" + address + ":" + 8765,
                    {
                        timeout: 30,
                        responseType: ResponseType.JSON,
                        headers: reqHeaders,
                    },
                );

                if (response.status == 403) {
                    const errMsg: ToastSettings = {
                        message: "Invalid server password!",
                        timeout: 5000,
                        background: "variant-filled-error",
                    };
                    toastStore.trigger(errMsg);
                } else {
                    lib = response.data;
                    sessionStorage.setItem("app-data", JSON.stringify(lib));
                }
                toastStore.close(tId);
            } catch (err: any) {
                const errMsg: ToastSettings = {
                    message: err,
                    timeout: 5000,
                    background: "variant-filled-error",
                };
                toastStore.trigger(errMsg);
            }
        } else {
            const errMsg: ToastSettings = {
                message: "Could not connect to server; invalid address",
                timeout: 5000,
                background: "variant-filled-error",
            };
            toastStore.trigger(errMsg);
        }
    }

    function parseName(inp: string): string {
        return inp.replaceAll("_", " ");
    }

    // On startup try to load
    onStart();
    onRefreshClick();
</script>

<Modal />
<Toast />

<AppShell>
    <svelte:fragment slot="sidebarLeft">
        <AppRail width="w-20" class="overflow-x-hidden">
            <svelte:fragment slot="lead">
                <AppRailAnchor href="/" selected={$page.url.pathname === "/"}>
                    <center>
                        <Icon icon="ph:gear" width="40" />
                    </center>
                </AppRailAnchor>
            </svelte:fragment>

            {#each lib.apps as app}
                <AppRailAnchor
                    href="/apps/{app.name}"
                    selected={$page.url.pathname == "/apps/" + app.name + "/"}
                    width="w-20"
                >
                    <span class="text-center w-full text-xs">
                        {parseName(app.name)}
                    </span>
                </AppRailAnchor>
            {/each}

            <svelte:fragment slot="trail">
                <div class="p-4">
                    <button
                        on:click={onRefreshClick}
                        type="button"
                        class="btn-icon variant-filled-primary"
                    >
                        <Icon icon="material-symbols:refresh" />
                    </button>
                </div>
            </svelte:fragment>
        </AppRail>
    </svelte:fragment>
    <center>
        <div class="card p-4 m-10">
            <slot />
        </div>
    </center>
    <svelte:fragment slot="pageFooter"
        ><p class="text-right text-slate-600">
            Gooselib v{appVersion}
        </p></svelte:fragment
    >
</AppShell>
