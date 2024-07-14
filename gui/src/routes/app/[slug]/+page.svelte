<script lang="ts">
    import { sendDownloadReq } from '$lib/download.js';
    import { getAppMetaData } from '$lib/metadata.js';
    import {
        triggerRemoveAppModal,
        triggerUpdateAppModal,
    } from '$lib/modals.js';
    import { formatBytes } from '$lib/util';
    import { getModalStore, getToastStore } from '@skeletonlabs/skeleton';
    import { invoke } from '@tauri-apps/api';

    export let data;

    const modalStore = getModalStore();
    const toastStore = getToastStore();

    let inst = false;
    let updt = false;

    $: installed = inst;
    $: needsUpdate = updt;

    async function checkIfInstalled() {
        await invoke('check_dir_exists', {
            dir: data.installPath,
        }).then((val) => {
            inst = !!val;
        });

        data.metaData = getAppMetaData(data.slug);
        const instVer = localStorage.getItem('installed-ver-' + data.slug);
        if (inst && instVer) {
            updt = !(instVer == data.metaData?.latestVersion);
        }
    }

    function remove() {
        modalStore.trigger(triggerRemoveAppModal(toastStore, data.installPath));
    }

    // Called every second
    async function update() {
        while (true) {
            await checkIfInstalled();
            await new Promise((resolve) => setTimeout(resolve, 1000));
        }
    }

    async function downloadStart() {
        if (data.metaData) {
            await sendDownloadReq(
                data.slug,
                data.metaData?.latestVersion,
                data.installPath
            );
        }
    }

    async function updateStart() {
        if (data.metaData) {
            modalStore.trigger(
                triggerUpdateAppModal(
                    toastStore,
                    data.installPath,
                    data.metaData.name,
                    data.metaData.latestVersion
                )
            );
        }
    }

    async function openFolder() {
        await invoke('open_path', {
            path: data.installPath,
        });
    }

    const iV = localStorage.getItem('installed-ver-' + data.slug);
    if (iV != null) inst = true;
    checkIfInstalled();
    update();
</script>

<div class="flex flex-col justify-between">
    <div>
        <h1 class="h1 mb-10">{data.slug}</h1>
        {#if installed && !needsUpdate}
            <button class="btn variant-filled-primary mb-10 mx-3">Run</button>
            <button class="btn variant-filled mb-10 mx-3" on:click={openFolder}
                >Open</button
            >
            <button
                class="btn variant-filled-error mb-10 mx-3"
                on:click={remove}>Delete</button
            >
        {:else if needsUpdate}
            <button class="btn variant-filled-primary mb-10 mx-3">Run</button>
            <button
                class="btn variant-filled-warning mb-10"
                on:click={updateStart}>Update</button
            >
        {:else}
            <button
                class="btn variant-filled-primary mb-10"
                on:click={downloadStart}>Install</button
            >
        {/if}
        <hr class="!border-t-4 my-2" />
    </div>
    <div class="p-4">
        <h4 class="h4 mb-4">App info</h4>
        <p class="text-slate-400">
            Latest version: {data.metaData?.latestVersion}
        </p>
        <p class="text-slate-400">Install folder: {data.installPath}</p>
        <p class="text-slate-400">
            App size (compressed): {data.metaData?.bytesCount} bytes ({formatBytes(
                data.metaData?.bytesCount,
                2
            )})
        </p>
    </div>
</div>