<script lang="ts">
    import { onDestroy, type SvelteComponent } from 'svelte';
    import { getModalStore, getToastStore } from '@skeletonlabs/skeleton';
    import { tryHandshake } from './login';
    import { loginErrToast, loginSuccToast } from './toasts';

    // Props
    /** Exposes parent props to this component. */
    export let parent: SvelteComponent;

    const modalStore = getModalStore();
    const toastStore = getToastStore();

    const formData = {
        ip: '',
        username: '',
        password: '',
    };

    const savUser = localStorage.getItem('username');
    const savPass = localStorage.getItem('password');
    const ip = localStorage.getItem('ip');

    if (savUser) formData.username = savUser;
    if (savPass) formData.password = savPass;
    if (ip) formData.ip = ip;

    sessionStorage.setItem('login-alive', 'true');

    async function onFormSubmit() {
        const succ = await tryHandshake(
            formData.username,
            formData.password,
            formData.ip
        );
        if (succ) {
            modalStore.close();
            sessionStorage.setItem('logged-in', 'true');
            toastStore.trigger(loginSuccToast);
        } else {
            toastStore.trigger(loginErrToast);
        }
    }

    // Cleanup function to be called when the modal is closed without submission
    function onModalClose() {
        console.log('Modal closed without submitting the form');
        sessionStorage.setItem('login-alive', 'false');
    }

    onDestroy(() => {
        if (sessionStorage.getItem('logged-in') == 'false') {
            onModalClose();
        }
    });

    // Base Classes
    const cBase = 'card p-4 w-modal shadow-xl space-y-4';
    const cHeader = 'text-2xl font-bold text-center';
    const cForm =
        'border border-surface-500 p-4 space-y-4 rounded-container-token';
</script>

{#if $modalStore[0]}
    <div class="modal-login-form {cBase}">
        <header class={cHeader}>Login to server</header>
        <!-- Enable for debugging: -->
        <form class="modal-form {cForm}">
            <label class="label">
                <span>IP address</span>
                <input
                    class="input"
                    type="text"
                    bind:value={formData.ip}
                    placeholder="..."
                />
            </label>
            <label class="label">
                <span>Username</span>
                <input
                    class="input"
                    type="tel"
                    bind:value={formData.username}
                    placeholder="..."
                />
            </label>
            <label class="label">
                <span>Password</span>
                <input
                    class="input"
                    type="password"
                    bind:value={formData.password}
                    placeholder="..."
                />
            </label>
        </form>
        <!-- prettier-ignore -->
        <footer class="modal-footer {parent.regionFooter}">
			<button class="btn {parent.buttonPositive}" on:click={onFormSubmit}>Login</button>
		</footer>
    </div>
{/if}
