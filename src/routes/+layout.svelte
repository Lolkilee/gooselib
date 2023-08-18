<script lang="ts">
    import "@skeletonlabs/skeleton/themes/theme-crimson.css";
    import "@skeletonlabs/skeleton/styles/skeleton.css";
    import "../app.postcss";
    import Icon from "@iconify/svelte";
    import { page } from "$app/stores";
    import {
        AppShell,
        AppRail,
        AppRailTile,
        AppRailAnchor,
        LightSwitch,
    } from "@skeletonlabs/skeleton";
</script>

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
                href="/about"
                selected={$page.url.pathname === "/about"}
            >
                (icon)
            </AppRailAnchor>

            <svelte:fragment slot="trail">
                <div class="card p-4">
                    <LightSwitch />
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
