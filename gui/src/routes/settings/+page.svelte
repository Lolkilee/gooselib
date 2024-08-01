<script lang="ts">
    import { SlideToggle } from '@skeletonlabs/skeleton';
    import { open } from '@tauri-apps/api/dialog';

    const themes = [
        'skeleton',
        'material',
        'wintry',
        'modern',
        'rocket',
        'seafoam',
        'vintage',
        'sahara',
        'hamlindigo',
        'gold-nouveau',
        'crimson',
    ];

    let selectedTheme = 'skeleton';
    let installFolder: string | null = localStorage.getItem('installFolder');

    const storedTheme = localStorage.getItem('theme');
    if (storedTheme != null) selectedTheme = storedTheme;

    $: document.body.dataset.theme = selectedTheme;
    $: localStorage.setItem('theme', selectedTheme);
    $: if (installFolder != null)
        localStorage.setItem('installFolder', installFolder);

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

<div class="flex justify-between my-2">
    <h5 class="h5 ml-4">Install folder</h5>
    <div class="flex w-2/3">
        <input
            class="input text-center py-2"
            type="text"
            placeholder="install folder"
            readonly={true}
            bind:value={installFolder}
        />
        <button
            type="button"
            class="btn variant-filled ml-2"
            on:click={setInstallFolder}>Browse</button
        >
    </div>
</div>

<div class="flex justify-between my-2">
    <h5 class="h5 ml-4">Theme</h5>
    <select class="select w-2/3" bind:value={selectedTheme}>
        {#each themes as theme}
            <option value={theme}>{theme}</option>
        {/each}
    </select>
</div>
