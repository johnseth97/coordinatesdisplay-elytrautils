# Chat Disabled

**Property:** `MC_DEBUG_CHAT_DISABLED`
**Tab:** Misc
**Requires restart:** Yes

## Description

Disables the in-game chat system entirely. Incoming and outgoing chat packets are dropped and the chat GUI is suppressed, simulating a server configuration where chat is fully turned off. System messages and command output are unaffected.

## When to use

Use when testing UI flows that need to verify behavior in a chat-disabled environment, or when running headless integration sessions where chat traffic would add noise to packet logs.
