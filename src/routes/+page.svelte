<script lang="ts">
    // Settings page

    import { toastStore, type ToastSettings } from "@skeletonlabs/skeleton";
    import Flex from "svelte-flex";

    let serverAddress: string = "";
    let serverPassword: string = "";

    const saveMsg: ToastSettings = {
        message: "Settings saved!",
        timeout: 2000,
        background: "variant-filled-primary",
    };

    if (localStorage.getItem("saved-address") != null)
        serverAddress = localStorage.getItem("saved-address");

    if (localStorage.getItem("server-password") != null)
        serverPassword = localStorage.getItem("server-password");

    function saveSettings() {
        localStorage.setItem("saved-address", serverAddress);
        localStorage.setItem("server-password", serverPassword);
        toastStore.trigger(saveMsg);
    }
</script>

<h1 class="h1 mb-12">Settings</h1>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5 ml-4">Server address</h5>
        <input
            class="input text-center w-3/4"
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
            class="input text-center w-3/4"
            type="password"
            placeholder="server password"
            bind:value={serverPassword}
        />
    </Flex>
</div>

<button
    on:click={saveSettings}
    type="button"
    class="btn variant-filled-primary mt-16"
>
    Save Settings
</button>
