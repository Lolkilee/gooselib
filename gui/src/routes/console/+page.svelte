<script lang="ts">
    import { BASE_URL } from '$lib/login';
    import { consoleLineStore, javaLineStore } from '$lib/stores';
    import { fetch, ResponseType } from '@tauri-apps/api/http';

    type Command = {
        name: string;
        description: string;
        call: (words: string[]) => void;
    };

    const commands: Command[] = [
        {
            name: 'help',
            description: 'prints all possible commands',
            call: printCommands,
        },
        {
            name: 'clear',
            description: 'clears the console',
            call: clearConsole,
        },
        {
            name: 'java',
            description: 'prints the full process log of the java subclient',
            call: showJavaLog,
        },
        {
            name: 'put-user',
            description:
                '<username> <password> <admin password>; sends a put user request to the server',
            call: putUser,
        },
        {
            name: 'set-exec',
            description:
                '<app name> <relative path> <admin password>; sends a set executable request to the server',
            call: setExecPath,
        },
        {
            name: 'upload',
            description:
                '<path> <app name> <admin password>; tries to start an upload with given arguments',
            call: uploadCommand,
        },
    ];

    let command = '';
    let output: string[] = [];
    let javaLog: string[] = [];

    consoleLineStore.subscribe((val) => {
        output = val;
    });

    javaLineStore.subscribe((val) => {
        javaLog = val;
    });

    function formatPath(path: string): string {
        return (
            '[' +
            path
                .replaceAll('/', '!')
                .replaceAll('\\', '!')
                .replaceAll(':', '*')
                .replaceAll('_', ' ') +
            ']'
        );
    }

    function parseCommand(line: string[]) {
        let executed = false;
        commands.forEach((comm) => {
            if (comm.name.toLowerCase() == line[0]) {
                comm.call(line);
                executed = true;
            }
        });

        if (!executed) {
            addLine(
                "could not recognize command, type 'help' for a list of commands"
            );
        }
    }

    function addLine(line: string) {
        consoleLineStore.set([...output, line + '\n']);
    }

    function handleCommand() {
        if (command.trim() !== '') {
            consoleLineStore.set([...output, '> ' + command + '\n']);
            parseCommand(command.split(' '));
            command = '';
        }
    }

    // ===== command functions =====
    function printCommands(_: string[]) {
        addLine('===== List of possible commands =====');
        commands.forEach((val) => {
            addLine(val.name + ': ' + val.description);
        });
    }

    function clearConsole(_: string[]) {
        consoleLineStore.set([]);
    }

    function showJavaLog(_: string[]) {
        javaLog.forEach((line) => {
            addLine(line);
        });
    }

    async function putUser(args: string[]) {
        if (args.length != 4) {
            addLine(
                "invalid arguments, expected 'put-user <username> <password> <admin password>'"
            );
            return;
        }

        const username = args[1];
        const password = args[2];
        const adminPass = args[3];

        const url =
            BASE_URL +
            'put-user/' +
            username +
            '/' +
            password +
            '/' +
            adminPass;

        const resp = await fetch(url, {
            method: 'POST',
            responseType: ResponseType.Text,
        });

        //@ts-ignore
        addLine(resp.data);
    }

    async function setExecPath(args: string[]) {
        if (args.length != 4) {
            addLine(
                "invalid arguments, expected 'set-exec <app name> <relative path> <admin password>'"
            );
            return;
        }

        const appName = args[1];
        const path = args[2];
        const adminPass = args[3];

        const formPath = formatPath(path);

        const url =
            BASE_URL +
            'set-exec-path/' +
            appName +
            '/' +
            formPath +
            '/' +
            adminPass;

        const resp = await fetch(url, {
            method: 'POST',
            responseType: ResponseType.Text,
        });

        //@ts-ignore
        addLine(resp.data);
    }

    async function uploadCommand(args: string[]) {
        if (args.length != 4) {
            addLine(
                "invalid arguments, expected 'upload <path> <app name> <admin password>'"
            );
            return;
        }

        const path = formatPath(args[1]);
        const name = args[2];
        const adminPass = args[3];

        const url = BASE_URL + 'upload/' + path + '/' + name + '/' + adminPass;

        const resp = await fetch(url, {
            method: 'POST',
            responseType: ResponseType.Text,
        });

        //@ts-ignore
        addLine(resp.data);
    }
</script>

<div class="h-full flex flex-col">
    <h1 class="h1">Console</h1>

    <div class="terminal bg-surface-900 flex flex-col justify-between h-5/6">
        <div class="terminal-output flex flex-col-reverse overflow-auto h-full">
            {#each output as line}
                {line}
            {/each}
        </div>
        <div class="prompt mt-4">
            <span class="prompt-symbol">$</span>
            <input
                id="commandInput"
                bind:value={command}
                on:keydown={(e) => e.key === 'Enter' && handleCommand()}
                placeholder="Type 'help' for a list of commands..."
            />
        </div>
    </div>
</div>

<style>
    .terminal {
        color: white;
        padding: 1em;
        border-radius: 5px;
        font-family: monospace;
        width: 100%;
        margin: auto;
    }

    .terminal-output {
        white-space: pre-wrap;
        overflow: auto;
    }

    .prompt {
        display: flex;
        align-items: center;
    }

    .prompt-symbol {
        margin-right: 0.5em;
    }

    input {
        background: transparent;
        border: none;
        color: white;
        outline: none;
        flex-grow: 1;
    }
</style>
