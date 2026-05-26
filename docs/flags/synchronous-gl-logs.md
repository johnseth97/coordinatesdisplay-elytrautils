# Synchronous GL Logs

**Property:** `MC_DEBUG_SYNCHRONOUS_GL_LOGS`
**Tab:** Gameplay
**Requires restart:** Yes

## Description

Makes OpenGL debug log callbacks fire synchronously on the render thread instead of asynchronously. Asynchronous delivery means GL errors may appear in the log after the offending draw call has already completed, making it difficult to correlate the error with the responsible code. Synchronous delivery ensures the error fires at exactly the point of the problematic call.

## When to use

Enable when investigating OpenGL errors or render artifacts and needing the error to appear at the exact call site rather than at an arbitrary later point.
