#include "cinderhell_audio.h"

#include <AL/al.h>
#include <AL/alext.h>
#include <jni.h>
#include <math.h>
#include <pthread.h>
#include <stdatomic.h>
#include <stdio.h>
#include <stdlib.h>

JNIEXPORT void JNICALL Java_dev_cinderhell_GameActivity_nativeSetAudioMuted(
    JNIEnv *env, jobject activity, jboolean muted);

#define CHECK(condition) do { \
    if (!(condition)) { \
        fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #condition); \
        exit(1); \
    } \
} while (0)

enum { FRAMES = 4096, RATE = 48000 };

static void AppMuted(bool muted)
{
    Java_dev_cinderhell_GameActivity_nativeSetAudioMuted(
        NULL, NULL, muted ? JNI_TRUE : JNI_FALSE);
}

static ALCcontext *NewContext(ALCdevice *device)
{
    const ALCint attributes[] = {
        ALC_FORMAT_CHANNELS_SOFT, ALC_STEREO_SOFT,
        ALC_FORMAT_TYPE_SOFT, ALC_FLOAT_SOFT,
        ALC_FREQUENCY, RATE,
        ALC_OUTPUT_LIMITER_SOFT, ALC_FALSE,
        0
    };
    ALCcontext *context = alcCreateContext(device, attributes);
    CHECK(context != NULL);
    return context;
}

static void StartSources(ALuint sources[2], ALuint buffers[2])
{
    ALshort samples[FRAMES * 2];
    alGenBuffers(2, buffers);
    alGenSources(2, sources);
    for (int channel = 0; channel < 2; ++channel)
    {
        for (int frame = 0; frame < FRAMES; ++frame)
        {
            samples[frame * 2 + channel] =
                (ALshort)(12000.0 * sin(frame * 0.1));
            samples[frame * 2 + 1 - channel] = 0;
        }
        alBufferData(buffers[channel], AL_FORMAT_STEREO16,
                     samples, sizeof(samples), RATE);
        alSourcei(sources[channel], AL_BUFFER, (ALint)buffers[channel]);
        alSourcei(sources[channel], AL_LOOPING, AL_TRUE);
        alSourcef(sources[channel], AL_GAIN, channel == 0 ? 0.25f : 0.5f);
    }
    alSourcePlayv(2, sources);
    CHECK(alGetError() == AL_NO_ERROR);
}

static void CheckOutput(ALCdevice *device, bool silent, const char *label)
{
    float samples[FRAMES * 2];
    double energy[2] = {0, 0};
    // Drain OpenAL's gain ramp and mixer pipeline before measuring steady state.
    alcRenderSamplesSOFT(device, samples, FRAMES);
    alcRenderSamplesSOFT(device, samples, FRAMES);
    for (int frame = 0; frame < FRAMES; ++frame)
    {
        for (int channel = 0; channel < 2; ++channel)
        {
            double sample = samples[frame * 2 + channel];
            CHECK(isfinite(sample));
            energy[channel] += sample * sample;
        }
    }
    for (int channel = 0; channel < 2; ++channel)
    {
        double rms = sqrt(energy[channel] / FRAMES);
        printf("%s channel=%d rms=%.8f\n", label, channel, rms);
        CHECK(silent ? rms < 0.000001 : rms > 0.01);
    }
    CHECK(alcGetError(device) == ALC_NO_ERROR);
}

static void CheckGain(ALCcontext *context, float expected)
{
    ALCcontext *previous = alcGetThreadContext();
    CHECK(alcSetThreadContext(context));
    ALfloat gain = -1;
    alGetListenerf(AL_GAIN, &gain);
    CHECK(gain == expected);
    CHECK(alGetError() == AL_NO_ERROR);
    CHECK(alcSetThreadContext(previous));
}

static void DestroyContext(ALCcontext *context)
{
    CinderhellAudio_Detach(context);
    CHECK(alcSetThreadContext(NULL));
    CHECK(alcMakeContextCurrent(NULL));
    alcDestroyContext(context);
}

static atomic_bool worker_running;
static atomic_int worker_updates;

static void *MuteWorker(void *unused)
{
    (void)unused;
    while (atomic_load(&worker_running))
    {
        AppMuted(true);
        AppMuted(false);
        CHECK(alcGetThreadContext() == NULL);
        atomic_fetch_add(&worker_updates, 1);
    }
    return NULL;
}

int main(void)
{
    CHECK(alcIsExtensionPresent(NULL, "ALC_SOFT_loopback"));
    ALCdevice *device = alcLoopbackOpenDeviceSOFT(NULL);
    CHECK(device != NULL);
    ALCcontext *context = NewContext(device);
    CHECK(alcMakeContextCurrent(context));
    CinderhellAudio_Attach(context);
    ALuint sources[2], buffers[2];
    StartSources(sources, buffers);
    CheckOutput(device, true, "default muted before Java resume");

    AppMuted(false);
    CheckOutput(device, false, "resume: both SFX/music sources audible");
    CinderhellAudio_SetWindowMuted(true);
    CinderhellAudio_SetWindowMuted(true);
    CheckOutput(device, true, "repeated window mute");
    AppMuted(true);
    CinderhellAudio_SetWindowMuted(false);
    CheckOutput(device, true, "window unmute cannot override call");
    CinderhellAudio_SetWindowMuted(true);
    AppMuted(false);
    CheckOutput(device, true, "call end cannot override window");
    CinderhellAudio_SetWindowMuted(false);
    CinderhellAudio_SetWindowMuted(false);
    CheckOutput(device, false, "both reasons cleared, repeat unmute");

    // A paused game thread may leave a deferred batch open indefinitely.
    alDeferUpdatesSOFT();
    AppMuted(true);
    CheckOutput(device, true, "call mute flushes deferred game updates");
    alDeferUpdatesSOFT();
    AppMuted(false);
    CheckOutput(device, false, "resume flushes deferred game updates");
    for (int i = 0; i < 2; ++i)
    {
        ALfloat gain;
        ALint state;
        alGetSourcef(sources[i], AL_GAIN, &gain);
        alGetSourcei(sources[i], AL_SOURCE_STATE, &state);
        CHECK(gain == (i == 0 ? 0.25f : 0.5f));
        CHECK(state == AL_PLAYING);
    }
    CHECK(alGetError() == AL_NO_ERROR);

    // Both process-current and caller-local contexts differ from the target.
    ALCdevice *other_device = alcLoopbackOpenDeviceSOFT(NULL);
    CHECK(other_device != NULL);
    ALCcontext *global_context = NewContext(other_device);
    ALCcontext *thread_context = NewContext(other_device);
    CHECK(alcMakeContextCurrent(global_context));
    CHECK(alcSetThreadContext(thread_context));
    AppMuted(true);
    CHECK(alcGetThreadContext() == thread_context);
    CheckGain(context, 0.0f);
    CheckGain(thread_context, 1.0f);
    CHECK(alcSetThreadContext(NULL));
    CHECK(alcGetCurrentContext() == global_context);
    CheckGain(global_context, 1.0f);
    AppMuted(false);
    CHECK(alcGetThreadContext() == NULL);
    CheckGain(context, 1.0f);
    CinderhellAudio_Detach(global_context); // Must not detach the game context.
    AppMuted(true);
    CheckGain(context, 0.0f);
    CHECK(alcMakeContextCurrent(NULL));
    alcDestroyContext(thread_context);
    alcDestroyContext(global_context);
    CHECK(alcCloseDevice(other_device));

    CHECK(alcMakeContextCurrent(context));
    alDeleteSources(2, sources);
    alDeleteBuffers(2, buffers);
    DestroyContext(context);
    // Updates without a context must survive until the next attach.
    AppMuted(false);
    AppMuted(true);
    context = NewContext(device);
    CHECK(alcMakeContextCurrent(context));
    CinderhellAudio_Attach(context);
    StartSources(sources, buffers);
    CheckOutput(device, true, "mute-before-attach retained across reinit");
    AppMuted(false);
    CheckOutput(device, false, "reinitialized sources resume");
    CinderhellAudio_SetWindowMuted(true);
    alDeleteSources(2, sources);
    alDeleteBuffers(2, buffers);
    DestroyContext(context);
    context = NewContext(device);
    CinderhellAudio_Attach(context);
    CheckGain(context, 0.0f);
    CinderhellAudio_SetWindowMuted(false);
    CheckGain(context, 1.0f);
    DestroyContext(context);

    // Race UI mute callbacks against native attach/detach/destruction.
    pthread_t worker;
    atomic_store(&worker_running, true);
    CHECK(pthread_create(&worker, NULL, MuteWorker, NULL) == 0);
    for (int iteration = 0; iteration < 100; ++iteration)
    {
        context = NewContext(device);
        CinderhellAudio_Attach(context);
        CinderhellAudio_SetWindowMuted(true);
        CheckGain(context, 0.0f); // Window mute wins regardless of worker state.
        CinderhellAudio_SetWindowMuted(false);
        DestroyContext(context);
    }
    atomic_store(&worker_running, false);
    CHECK(pthread_join(worker, NULL) == 0);
    CHECK(atomic_load(&worker_updates) > 0);
    AppMuted(true); // Must not touch any destroyed context.
    CHECK(alcCloseDevice(device));
    printf("PASS: loopback audio, mute overlap, context isolation, reinit, "
           "100 teardown races (%d UI updates)\n", atomic_load(&worker_updates));
    return 0;
}
