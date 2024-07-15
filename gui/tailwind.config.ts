import { join } from 'path'
import type { Config } from 'tailwindcss'
import { skeleton } from '@skeletonlabs/tw-plugin'
import forms from '@tailwindcss/forms'
import { materialTheme } from './src/themes/material';

export default {
    darkMode: 'class',
    content: ['./src/**/*.{html,js,svelte,ts}', join(require.resolve('@skeletonlabs/skeleton'), '../**/*.{html,js,svelte,ts}')],
    theme: {
        extend: {},
    },
    plugins: [
        forms,
        skeleton({
            themes: {
                custom: [
                    materialTheme
                ],
                preset: [
                    {
                        name: 'skeleton',
                        enhancements: true,
                    },
                    {
                        name: 'wintry',
                        enhancements: true,
                    },
                    {
                        name: 'modern',
                        enhancements: true,
                    },
                    {
                        name: 'hamlindigo',
                        enhancements: true,
                    },
                    {
                        name: 'rocket',
                        enhancements: true,
                    },
                    {
                        name: 'sahara',
                        enhancements: true,
                    },
                    {
                        name: 'gold-nouveau',
                        enhancements: true,
                    },
                    {
                        name: 'vintage',
                        enhancements: true,
                    },
                    {
                        name: 'seafoam',
                        enhancements: true,
                    },
                    {
                        name: 'crimson',
                        enhancements: true,
                    },
                ],
            },
        }),
    ],
} satisfies Config;
