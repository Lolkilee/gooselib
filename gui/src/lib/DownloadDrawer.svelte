<script lang="ts">
    import { ProgressBar } from '@skeletonlabs/skeleton';
    import type { DownloadInfo } from './download';
    import { downloadInfoStore } from './stores';

    let downloads: DownloadInfo[] = [];

    $: infos = downloads;
</script>

<div class="p-8">
    <h1 class="h2 mb-10">Active Downloads</h1>
    {#each infos as info}
        <div class="flex card p-4 my-2 justify-between">
            <p class="pt-2">{info.appName}</p>
            <div class="flex py-2 w-3/4">
                <p class="pr-10">{Math.round(info.progress * 100)}%</p>
                <ProgressBar
                    class="mt-2"
                    value={info.progress * 100}
                    max={100}
                />
            </div>
        </div>
    {/each}
</div>
