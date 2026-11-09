#ifndef CINDERHELL_AUDIO_H
#define CINDERHELL_AUDIO_H

#include <stdbool.h>
#include <AL/alc.h>

// The native audio owner attaches a current, valid context before creating
// sources and detaches it before destruction. Lifecycle calls stay on that
// owner thread; mute calls may run concurrently on the Java UI thread.
// Both mute reasons survive detach/reinit. App mute starts true until Java
// supplies the resumed/call state through nativeSetAudioMuted(boolean).
void CinderhellAudio_Attach(ALCcontext *context);
void CinderhellAudio_Detach(ALCcontext *context);
void CinderhellAudio_SetWindowMuted(bool muted);

#endif
