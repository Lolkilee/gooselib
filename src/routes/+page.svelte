<script lang="ts">
    // Settings page
    import {
        toastStore,
        type ToastSettings,
        LightSwitch,
        type ModalSettings,
        modalStore,
    } from "@skeletonlabs/skeleton";
    // @ts-ignore
    import Flex from "svelte-flex";
    import { open } from "@tauri-apps/api/dialog";
    import { Command } from "@tauri-apps/api/shell";

    const themes = [
        "skeleton",
        "wintry",
        "modern",
        "rocket",
        "seafoam",
        "vintage",
        "sahara",
        "hamlindigo",
        "gold-nouveau",
        "crimson",
    ];

    let selectedTheme = "crimson";

    $: selectedTheme && changeTheme(selectedTheme);

    let serverAddress: string | null = "";
    let serverPassword: string | null = "";
    let installFolder: string | null = "";
    let uploadStatus: "";
    let disableUpload = false;

    let folder: string, password: string, appName: string, version: string;

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

    const s = localStorage.getItem("saved-theme");
    if (s != null) selectedTheme = s;

    function saveSettings() {
        if (serverAddress != null)
            localStorage.setItem("saved-address", serverAddress);
        if (serverPassword != null)
            localStorage.setItem("server-password", serverPassword);
        if (installFolder != null)
            localStorage.setItem("install-folder", installFolder);
        if (selectedTheme != null)
            localStorage.setItem("saved-theme", selectedTheme);
        toastStore.trigger(saveMsg);
    }

    function changeTheme(nTheme: string) {
        document.body.dataset.theme = nTheme;
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

    function uploadToServer() {
        modalStore.trigger(passModal);
    }

    async function selectUploadFolder() {
        const selected = await open({
            multiple: false,
            directory: true,
        });
        if (!Array.isArray(selected) && selected != null) {
            folder = selected;
            startUpload(folder, password, appName, version);
        }
    }

    async function startUpload(
        folder: string,
        password: string,
        appName: string,
        version: string,
    ) {
        let url = localStorage.getItem("saved-address");
        if (url != null) {
            const uploadCommand = Command.sidecar("../svr/build/gl-packager", [
                folder,
                password,
                appName,
                version,
                "http://" + url + ":8765",
            ]);

            uploadCommand.stdout.on("data", (line) => {
                console.log(line);
                uploadStatus = line;
            });

            await uploadCommand.execute();
        }
    }

    // Modals
    const passModal: ModalSettings = {
        type: "prompt",
        title: "Enter upload values",
        body: "Type in the upload password below",
        value: "",
        valueAttr: { type: "password", required: true },

        response: (r: string) => {
            if (r != "") {
                password = r;
                modalStore.trigger(nameModal);
            }
        },
    };

    const nameModal: ModalSettings = {
        type: "prompt",
        title: "Enter upload values",
        body: "Type in the app name below (Use `_` for spaces)",
        value: "",
        valueAttr: { type: "text", required: true },

        response: (r: string) => {
            if (r != "") {
                appName = r;
                modalStore.trigger(versionModal);
            }
        },
    };

    const versionModal: ModalSettings = {
        type: "prompt",
        title: "Enter upload values",
        body: "Type in the app version below (spaces not allowed!)",
        value: "",
        valueAttr: { type: "text", required: true },

        response: async (r: string) => {
            if (r != "") {
                version = r;
                await selectUploadFolder();
            }
        },
    };
</script>

<h1 class="h1 mb-12">Settings</h1>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5 ml-4">Theme</h5>
        <select class="select w-2/3" bind:value={selectedTheme}>
            {#each themes as theme}
                <option value={theme}>{theme}</option>
            {/each}
        </select>
    </Flex>
</div>

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5 ml-4">Server address</h5>
        <input
            class="input text-center w-2/3 py-2"
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
            class="input text-center w-2/3 py-2"
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
                class="input text-center w-3/4 py-2"
                type="text"
                placeholder="install folder"
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

<div class="my-2">
    <Flex justify="between">
        <h5 class="h5 ml-4">Upload to server</h5>
        <div class="w-2/3">
            <input
                class="input text-center w-3/4 py-2"
                type="text"
                placeholder="..."
                readonly={true}
                bind:value={uploadStatus}
            />
            <button
                disabled={disableUpload}
                type="button"
                class="btn variant-filled"
                on:click={uploadToServer}>Upload</button
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
