<script lang="ts">
    import '../app.postcss';

    import { Child, Command } from '@tauri-apps/api/shell';
    import { resolveResource } from '@tauri-apps/api/path';
    import { fetch, ResponseType } from '@tauri-apps/api/http';
    import { listen } from '@tauri-apps/api/event';

    let clientProc: Child | null = null;

    async function keepAliveReq() {
        await fetch('http://localhost:7123/', {
            method: 'GET',
            responseType: ResponseType.Text,
        });
    }

    async function startClient() {
        let resourcePath: string = await resolveResource('java/');
        resourcePath = resourcePath.replace('\\\\?\\', '');
        console.log('starting jar from: ' + resourcePath);
        const comm = Command.sidecar('../binaries/exec_client', [resourcePath]);
        clientProc = await comm.spawn();

        // Test request
        const resp = await fetch('http://localhost:7123/', {
            method: 'GET',
            responseType: ResponseType.Text,
        });
        console.log(resp);
    }

    setInterval(keepAliveReq, 1000);
    startClient();
</script>

<slot />
