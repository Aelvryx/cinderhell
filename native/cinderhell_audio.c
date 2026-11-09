#include "cinderhell_audio.h"

#ifndef AL_ALEXT_PROTOTYPES
#define AL_ALEXT_PROTOTYPES
#endif
#include <AL/al.h>
#include <AL/alext.h>
#include <jni.h>
#include <pthread.h>

static pthread_mutex_t audio_mutex = PTHREAD_MUTEX_INITIALIZER;
static ALCcontext *audio_context;
static bool app_muted = true;
static bool window_muted;

// audio_mutex covers both the state and every use of the borrowed context.
// Detach therefore waits for in-flight UI calls before the owner destroys it.
static void ApplyMuteLocked(void)
{
    if (audio_context == NULL)
    {
        return;
    }

    ALCcontext *previous = alcGetThreadContext();
    if (!alcSetThreadContext(audio_context))
    {
        return;
    }

    alListenerf(AL_GAIN, app_muted || window_muted ? 0.0f : 1.0f);
    // Woof batches properties with AL_SOFT_deferred_updates. Flush so a UI
    // mute takes effect even if the game thread is suspended mid-batch. Do not
    // re-defer: the game thread could already have finished its batch.
    alProcessUpdatesSOFT();
    alcSetThreadContext(previous);
}

void CinderhellAudio_Attach(ALCcontext *context)
{
    pthread_mutex_lock(&audio_mutex);
    audio_context = context;
    ApplyMuteLocked();
    pthread_mutex_unlock(&audio_mutex);
}

void CinderhellAudio_Detach(ALCcontext *context)
{
    pthread_mutex_lock(&audio_mutex);
    if (audio_context == context)
    {
        audio_context = NULL;
    }
    pthread_mutex_unlock(&audio_mutex);
}

void CinderhellAudio_SetWindowMuted(bool muted)
{
    pthread_mutex_lock(&audio_mutex);
    window_muted = muted;
    ApplyMuteLocked();
    pthread_mutex_unlock(&audio_mutex);
}

JNIEXPORT void JNICALL Java_dev_cinderhell_GameActivity_nativeSetAudioMuted(
    JNIEnv *env, jobject activity, jboolean muted)
{
    (void)env;
    (void)activity;
    pthread_mutex_lock(&audio_mutex);
    app_muted = muted != JNI_FALSE;
    ApplyMuteLocked();
    pthread_mutex_unlock(&audio_mutex);
}
