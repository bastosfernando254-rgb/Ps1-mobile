#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <stdint.h>
#include <vector>
#include <string>
#include "libretro.h"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "PS1Mobile", __VA_ARGS__)
static JavaVM* vm=nullptr;
static jclass activityClass=nullptr;
static jmethodID videoMethod=nullptr, audioMethod=nullptr;
static void* core=nullptr;
static std::string systemDir, saveDir;
static int16_t buttons[16]={0};
static int16_t analogX=0, analogY=0;
static bool gameLoaded=false;

#define DECL(name) static decltype(&name) p_##name=nullptr
DECL(retro_set_environment); DECL(retro_set_video_refresh); DECL(retro_set_audio_sample);
DECL(retro_set_audio_sample_batch); DECL(retro_set_input_poll); DECL(retro_set_input_state);
DECL(retro_init); DECL(retro_deinit); DECL(retro_load_game); DECL(retro_unload_game);
DECL(retro_run); DECL(retro_get_system_av_info); DECL(retro_set_controller_port_device);

static bool RETRO_CALLCONV environment(unsigned cmd, void* data) {
    switch(cmd) {
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            if(data) *static_cast<bool*>(data)=true;
            return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            if(data) *static_cast<retro_pixel_format*>(data)=RETRO_PIXEL_FORMAT_XRGB8888;
            return true;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            if(data) *static_cast<const char**>(data)=systemDir.c_str();
            return true;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            if(data) *static_cast<const char**>(data)=saveDir.c_str();
            return true;
        default: return false;
    }
}

static void RETRO_CALLCONV video(const void* data, unsigned w, unsigned h, size_t pitch) {
    if(!data || !vm || !activityClass || !videoMethod || !w || !h || w>2048 || h>2048) return;
    JNIEnv* e=nullptr; bool detach=false;
    if(vm->GetEnv((void**)&e, JNI_VERSION_1_6)!=JNI_OK) {
        if(vm->AttachCurrentThread(&e, nullptr)!=JNI_OK) return;
        detach=true;
    }
    std::vector<jint> pixels((size_t)w*h);
    const uint8_t* src=(const uint8_t*)data;
    for(unsigned y=0;y<h;y++) {
        const uint32_t* row=(const uint32_t*)(src+y*pitch);
        for(unsigned x=0;x<w;x++) pixels[(size_t)y*w+x]=(jint)(0xFF000000u | (row[x]&0x00FFFFFFu));
    }
    jintArray arr=e->NewIntArray((jsize)pixels.size());
    if(arr) {
        e->SetIntArrayRegion(arr,0,(jsize)pixels.size(),pixels.data());
        e->CallStaticVoidMethod(activityClass,videoMethod,arr,(jint)w,(jint)h);
        e->DeleteLocalRef(arr);
    }
    if(e->ExceptionCheck()) e->ExceptionClear();
    if(detach) vm->DetachCurrentThread();
}
static void RETRO_CALLCONV audioOne(int16_t, int16_t) {}
static size_t RETRO_CALLCONV audioBatch(const int16_t* data, size_t frames) {
    if(!data || !vm || !activityClass || !audioMethod || !frames || frames>16384) return frames;
    JNIEnv* e=nullptr; bool detach=false;
    if(vm->GetEnv((void**)&e,JNI_VERSION_1_6)!=JNI_OK) {
        if(vm->AttachCurrentThread(&e,nullptr)!=JNI_OK) return frames;
        detach=true;
    }
    jshortArray arr=e->NewShortArray((jsize)(frames*2));
    if(arr) {
        e->SetShortArrayRegion(arr,0,(jsize)(frames*2),(const jshort*)data);
        e->CallStaticVoidMethod(activityClass,audioMethod,arr);
        e->DeleteLocalRef(arr);
    }
    if(e->ExceptionCheck()) e->ExceptionClear();
    if(detach) vm->DetachCurrentThread();
    return frames;
}
static void RETRO_CALLCONV inputPoll() {}
static int16_t RETRO_CALLCONV inputState(unsigned, unsigned device, unsigned index, unsigned id) {
    if(device==RETRO_DEVICE_JOYPAD && id<16) return buttons[id];
    if(device==RETRO_DEVICE_ANALOG && index==RETRO_DEVICE_INDEX_ANALOG_LEFT) {
        if(id==RETRO_DEVICE_ID_ANALOG_X) return analogX;
        if(id==RETRO_DEVICE_ID_ANALOG_Y) return analogY;
    }
    return 0;
}
static bool symbols() {
#define LOAD(n) p_##n=(decltype(p_##n))dlsym(core,#n); if(!p_##n) { LOGE("Core sem símbolo: %s",#n); return false; }
    LOAD(retro_set_environment); LOAD(retro_set_video_refresh); LOAD(retro_set_audio_sample);
    LOAD(retro_set_audio_sample_batch); LOAD(retro_set_input_poll); LOAD(retro_set_input_state);
    LOAD(retro_init); LOAD(retro_deinit); LOAD(retro_load_game); LOAD(retro_unload_game);
    LOAD(retro_run); LOAD(retro_get_system_av_info); LOAD(retro_set_controller_port_device);
#undef LOAD
    return true;
}
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* v, void*) { vm=v; return JNI_VERSION_1_6; }

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ps1mobile_MainActivity_nativeInit(JNIEnv* e, jclass cls, jstring corePath, jstring sys, jstring saves) {
    const char* c=e->GetStringUTFChars(corePath,nullptr);
    const char* s=e->GetStringUTFChars(sys,nullptr);
    const char* d=e->GetStringUTFChars(saves,nullptr);
    systemDir=s; saveDir=d;
    core=dlopen(c,RTLD_NOW|RTLD_LOCAL);
    e->ReleaseStringUTFChars(corePath,c); e->ReleaseStringUTFChars(sys,s); e->ReleaseStringUTFChars(saves,d);
    if(!core || !symbols()) { LOGE("Falha ao carregar núcleo: %s",dlerror()); return JNI_FALSE; }
    activityClass=(jclass)e->NewGlobalRef(cls);
    videoMethod=e->GetStaticMethodID(cls,"onVideoFrame","([III)V");
    audioMethod=e->GetStaticMethodID(cls,"onAudio","([S)V");
    p_retro_set_environment(environment);
    p_retro_set_video_refresh(video);
    p_retro_set_audio_sample(audioOne);
    p_retro_set_audio_sample_batch(audioBatch);
    p_retro_set_input_poll(inputPoll);
    p_retro_set_input_state(inputState);
    p_retro_init();
    p_retro_set_controller_port_device(0,RETRO_DEVICE_JOYPAD);
    return JNI_TRUE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_ps1mobile_MainActivity_nativeLoadGame(JNIEnv* e,jclass,jstring filePath) {
    if(!core) return JNI_FALSE;
    if(gameLoaded) { p_retro_unload_game(); gameLoaded=false; }
    const char* path=e->GetStringUTFChars(filePath,nullptr);
    retro_game_info info{}; info.path=path;
    bool ok=p_retro_load_game(&info);
    e->ReleaseStringUTFChars(filePath,path);
    gameLoaded=ok;
    return ok?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL Java_com_ps1mobile_MainActivity_nativeRunFrame(JNIEnv*,jclass) { if(gameLoaded) p_retro_run(); }
extern "C" JNIEXPORT void JNICALL Java_com_ps1mobile_MainActivity_nativeSetButton(JNIEnv*,jclass,jint id,jboolean down) {
    if(id>=0 && id<16) buttons[id]=down?1:0;
}
extern "C" JNIEXPORT void JNICALL Java_com_ps1mobile_MainActivity_nativeSetAnalog(JNIEnv*,jclass,jint x,jint y) {
    analogX=(int16_t)x; analogY=(int16_t)y;
}
extern "C" JNIEXPORT void JNICALL Java_com_ps1mobile_MainActivity_nativeShutdown(JNIEnv*,jclass) {
    if(gameLoaded) { p_retro_unload_game(); gameLoaded=false; }
    if(core) { p_retro_deinit(); dlclose(core); core=nullptr; }
}
