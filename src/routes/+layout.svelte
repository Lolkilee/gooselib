<script lang="ts">
    import "@skeletonlabs/skeleton/themes/theme-crimson.css";
    import "@skeletonlabs/skeleton/styles/skeleton.css";
    import "../app.postcss";
    import Icon from "@iconify/svelte";
    import { page } from "$app/stores";
    import { getClient, ResponseType } from "@tauri-apps/api/http";
    import {
        AppShell,
        AppRail,
        AppRailAnchor,
        toastStore,
        type ToastSettings,
        Toast,
    } from "@skeletonlabs/skeleton";

    function onRefreshClick() {
        //TODO
    }
</script>

<Toast />

<AppShell>
    <svelte:fragment slot="sidebarLeft">
        <AppRail>
            <svelte:fragment slot="lead">
                <AppRailAnchor href="/" selected={$page.url.pathname === "/"}>
                    <center>
                        <Icon icon="ph:gear" width="40" />
                    </center>
                </AppRailAnchor>
            </svelte:fragment>

            <AppRailAnchor
                href="/apps/test"
                selected={$page.url.pathname === "/apps/test"}
            >
                (icon)
            </AppRailAnchor>

            <svelte:fragment slot="trail">
                <div class="card p-4">
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
</AppShell>
