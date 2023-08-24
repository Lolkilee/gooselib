<script lang="ts">
    // Settings page

    import {
        toastStore,
        type ToastSettings,
        LightSwitch,
    } from "@skeletonlabs/skeleton";
    // @ts-ignore
    import Flex from "svelte-flex";
    import { open } from "@tauri-apps/api/dialog";

    let serverAddress: string | null = "";
    let serverPassword: string | null = "";
    let installFolder: string | null = "";

    const saveMsg: ToastSettings = {
        message: "Settings saved!",
        timeout: 2000,
        background: "variant-filled-primary",
    };

    if (localStorage.getItem("saved-address") != null)
        serverAddress = localStorage.getItem("saved-address");

    if (localStorage.getItem("server-password") != null)
        serverPassword = localStorage.getItem("server-password");

    if (localStorage.getItem("install-folder") != null)
        installFolder = localStorage.getItem("install-folder");

    function saveSettings() {
        if (serverAddress != null)
            localStorage.setItem("saved-address", serverAddress);
        if (serverPassword != null)
            localStorage.setItem("server-password", serverPassword);
        if (installFolder != null)
            localStorage.setItem("install-folder", installFolder);
        toastStore.trigger(saveMsg);
    }

    async function setInstallFolder() {
        const selected = await open({
            multiple: false,
            directory: true,
        });
        if (!Array.isArray(selected)) {
            installFolder = selected;
        }
    }
</script>

<h1 class="h1 mb-12">Settings</h1>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5 ml-4">Server address</h5>
        <input
            class="input text-center w-2/3"
            type="text"
            placeholder="server address"
            bind:value={serverAddress}
        />
    </Flex>
</div>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5 ml-4">Password</h5>
        <input
            class="input text-center w-2/3"
            type="password"
            placeholder="server password"
            bind:value={serverPassword}
        />
    </Flex>
</div>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5 ml-4">Install folder</h5>
        <div class="w-2/3">
            <input
                class="input text-center w-3/4"
                type="text"
                placeholder="server password"
                readonly={true}
                bind:value={installFolder}
            />
            <button
                type="button"
                class="btn variant-filled"
                on:click={setInstallFolder}>Browse</button
            >
        </div>
    </Flex>
</div>

<button
    on:click={saveSettings}
    type="button"
    class="btn variant-filled-primary mt-4"
>
    Save Settings
</button>
