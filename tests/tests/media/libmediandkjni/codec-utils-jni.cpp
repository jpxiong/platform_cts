/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Original code copied from NDK Native-media sample code */

#undef NDEBUG
//#define LOG_NDEBUG 0
#include <stdint.h>
#include <sys/types.h>
#include <jni.h>

#include <ScopedLocalRef.h>
#include <JNIHelp.h>

typedef ssize_t offs_t;

// for __android_log_print(ANDROID_LOG_INFO, "YourApp", "formatted message");
#include <android/log.h>
#define TAG "CodecUtilsJNI"
#define __ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#if LOG_NDEBUG
#define ALOGV(...) do { if (0) { __ALOGV(__VA_ARGS__); } } while (0)
#else
#define ALOGV(...) __ALOGV(__VA_ARGS__)
#endif
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct NativeImage {
    struct crop {
        int left;
        int top;
        int right;
        int bottom;
    } crop;
    struct plane {
        const uint8_t *buffer;
        size_t size;
        ssize_t colInc;
        ssize_t rowInc;
        offs_t cropOffs;
        size_t cropWidth;
        size_t cropHeight;
    } plane[3];
    int width;
    int height;
    int format;
    long timestamp;
    size_t numPlanes;
};

struct ChecksumAlg {
    virtual void init() = 0;
    virtual void update(uint8_t c) = 0;
    virtual uint32_t checksum() = 0;
    virtual size_t length() = 0;
protected:
    virtual ~ChecksumAlg() {}
};

struct Adler32 : ChecksumAlg {
    Adler32() {
        init();
    }
    void init() {
        a = 1;
        len = b = 0;
    }
    void update(uint8_t c) {
        a += c;
        b += a;
        ++len;
    }
    uint32_t checksum() {
        return (a % 65521) + ((b % 65521) << 16);
    }
    size_t length() {
        return len;
    }
private:
    uint32_t a, b;
    size_t len;
};

static struct ImageFieldsAndMethods {
    // android.graphics.ImageFormat
    int YUV_420_888;
    // android.media.Image
    jmethodID methodWidth;
    jmethodID methodHeight;
    jmethodID methodFormat;
    jmethodID methodTimestamp;
    jmethodID methodPlanes;
    jmethodID methodCrop;
    // android.media.Image.Plane
    jmethodID methodBuffer;
    jmethodID methodPixelStride;
    jmethodID methodRowStride;
    // android.graphics.Rect
    jfieldID fieldLeft;
    jfieldID fieldTop;
    jfieldID fieldRight;
    jfieldID fieldBottom;
} gFields;
static bool gFieldsInitialized = false;

void initializeGlobalFields(JNIEnv *env) {
    if (gFieldsInitialized) {
        return;
    }
    {   // ImageFormat
        jclass imageFormatClazz = env->FindClass("android/graphics/ImageFormat");
        const jfieldID fieldYUV420888 = env->GetStaticFieldID(imageFormatClazz, "YUV_420_888", "I");
        gFields.YUV_420_888 = env->GetStaticIntField(imageFormatClazz, fieldYUV420888);
        env->DeleteLocalRef(imageFormatClazz);
        imageFormatClazz = NULL;
    }

    {   // Image
        jclass imageClazz = env->FindClass("android/media/Image");
        gFields.methodWidth  = env->GetMethodID(imageClazz, "getWidth", "()I");
        gFields.methodHeight = env->GetMethodID(imageClazz, "getHeight", "()I");
        gFields.methodFormat = env->GetMethodID(imageClazz, "getFormat", "()I");
        gFields.methodTimestamp = env->GetMethodID(imageClazz, "getTimestamp", "()J");
        gFields.methodPlanes = env->GetMethodID(
                imageClazz, "getPlanes", "()[Landroid/media/Image$Plane;");
        gFields.methodCrop   = env->GetMethodID(
                imageClazz, "getCropRect", "()Landroid/graphics/Rect;");
        env->DeleteLocalRef(imageClazz);
        imageClazz = NULL;
    }

    {   // Image.Plane
        jclass planeClazz = env->FindClass("android/media/Image$Plane");
        gFields.methodBuffer = env->GetMethodID(planeClazz, "getBuffer", "()Ljava/nio/ByteBuffer;");
        gFields.methodPixelStride = env->GetMethodID(planeClazz, "getPixelStride", "()I");
        gFields.methodRowStride = env->GetMethodID(planeClazz, "getRowStride", "()I");
        env->DeleteLocalRef(planeClazz);
        planeClazz = NULL;
    }

    {   // Rect
        jclass rectClazz = env->FindClass("android/graphics/Rect");
        gFields.fieldLeft   = env->GetFieldID(rectClazz, "left", "I");
        gFields.fieldTop    = env->GetFieldID(rectClazz, "top", "I");
        gFields.fieldRight  = env->GetFieldID(rectClazz, "right", "I");
        gFields.fieldBottom = env->GetFieldID(rectClazz, "bottom", "I");
        env->DeleteLocalRef(rectClazz);
        rectClazz = NULL;
    }
    gFieldsInitialized = true;
}

NativeImage *getNativeImage(JNIEnv *env, jobject image) {
    if (image == NULL) {
        jniThrowNullPointerException(env, "image is null");
        return NULL;
    }

    initializeGlobalFields(env);

    NativeImage *img = new NativeImage;
    img->format = env->CallIntMethod(image, gFields.methodFormat);
    img->width  = env->CallIntMethod(image, gFields.methodWidth);
    img->height = env->CallIntMethod(image, gFields.methodHeight);
    img->timestamp = env->CallLongMethod(image, gFields.methodTimestamp);

    jobject cropRect = env->CallObjectMethod(image, gFields.methodCrop);
    img->crop.left   = env->GetIntField(cropRect, gFields.fieldLeft);
    img->crop.top    = env->GetIntField(cropRect, gFields.fieldTop);
    img->crop.right  = env->GetIntField(cropRect, gFields.fieldRight);
    img->crop.bottom = env->GetIntField(cropRect, gFields.fieldBottom);
    if (img->crop.right == 0 && img->crop.bottom == 0) {
        img->crop.right  = img->width;
        img->crop.bottom = img->height;
    }
    env->DeleteLocalRef(cropRect);
    cropRect = NULL;

    if (img->format != gFields.YUV_420_888) {
        jniThrowException(
                env, "java/lang/UnsupportedOperationException",
                "only support YUV_420_888 images");
        delete img;
        img = NULL;
        return NULL;
    }
    img->numPlanes = 3;

    ScopedLocalRef<jobjectArray> planesArray(
            env, (jobjectArray)env->CallObjectMethod(image, gFields.methodPlanes));
    int xDecim = 0;
    int yDecim = 0;
    for (size_t ix = 0; ix < img->numPlanes; ++ix) {
        ScopedLocalRef<jobject> plane(
                env, env->GetObjectArrayElement(planesArray.get(), (jsize)ix));
        img->plane[ix].colInc = env->CallIntMethod(plane.get(), gFields.methodPixelStride);
        img->plane[ix].rowInc = env->CallIntMethod(plane.get(), gFields.methodRowStride);
        ScopedLocalRef<jobject> buffer(
                env, env->CallObjectMethod(plane.get(), gFields.methodBuffer));

        img->plane[ix].buffer = (const uint8_t *)env->GetDirectBufferAddress(buffer.get());
        img->plane[ix].size = env->GetDirectBufferCapacity(buffer.get());

        img->plane[ix].cropOffs =
            (img->crop.left >> xDecim) * img->plane[ix].colInc
                    + (img->crop.top >> yDecim) * img->plane[ix].rowInc;
        img->plane[ix].cropHeight =
            ((img->crop.bottom + (1 << yDecim) - 1) >> yDecim) - (img->crop.top >> yDecim);
        img->plane[ix].cropWidth =
            ((img->crop.right + (1 << xDecim) - 1) >> xDecim) - (img->crop.left >> xDecim);

        // sanity check on increments
        ssize_t widthOffs =
            (((img->width + (1 << xDecim) - 1) >> xDecim) - 1) * img->plane[ix].colInc;
        ssize_t heightOffs =
            (((img->height + (1 << yDecim) - 1) >> yDecim) - 1) * img->plane[ix].rowInc;
        if (widthOffs < 0 || heightOffs < 0
                || widthOffs + heightOffs >= (ssize_t)img->plane[ix].size) {
            jniThrowException(
                    env, "java/lang/IndexOutOfBoundsException", "plane exceeds bytearray");
            delete img;
            img = NULL;
            return NULL;
        }
        xDecim = yDecim = 1;
    }
    return img;
}

extern "C" jint Java_android_media_cts_CodecUtils_getImageChecksum(JNIEnv *env,
        jclass /*clazz*/, jobject image)
{
    NativeImage *img = getNativeImage(env, image);
    if (img == NULL) {
        return 0;
    }

    Adler32 adler;
    for (size_t ix = 0; ix < img->numPlanes; ++ix) {
        const uint8_t *row = img->plane[ix].buffer + img->plane[ix].cropOffs;
        for (size_t y = img->plane[ix].cropHeight; y > 0; --y) {
            const uint8_t *col = row;
            ssize_t colInc = img->plane[ix].colInc;
            for (size_t x = img->plane[ix].cropWidth; x > 0; --x) {
                adler.update(*col);
                col += colInc;
            }
            row += img->plane[ix].rowInc;
        }
    }
    ALOGV("adler %zu/%u", adler.length(), adler.checksum());
    return adler.checksum();
}

/* tiled copy that loops around source image boundary */
extern "C" void Java_android_media_cts_CodecUtils_copyFlexYUVImage(JNIEnv *env,
        jclass /*clazz*/, jobject target, jobject source)
{
    NativeImage *tgt = getNativeImage(env, target);
    NativeImage *src = getNativeImage(env, source);
    if (tgt != NULL && src != NULL) {
        ALOGV("copyFlexYUVImage %dx%d (%d,%d..%d,%d) (%zux%zu) %+zd%+zd %+zd%+zd %+zd%+zd <= "
                "%dx%d (%d, %d..%d, %d) (%zux%zu) %+zd%+zd %+zd%+zd %+zd%+zd",
                tgt->width, tgt->height,
                tgt->crop.left, tgt->crop.top, tgt->crop.right, tgt->crop.bottom,
                tgt->plane[0].cropWidth, tgt->plane[0].cropHeight,
                tgt->plane[0].rowInc, tgt->plane[0].colInc,
                tgt->plane[1].rowInc, tgt->plane[1].colInc,
                tgt->plane[2].rowInc, tgt->plane[2].colInc,
                src->width, src->height,
                src->crop.left, src->crop.top, src->crop.right, src->crop.bottom,
                src->plane[0].cropWidth, src->plane[0].cropHeight,
                src->plane[0].rowInc, src->plane[0].colInc,
                src->plane[1].rowInc, src->plane[1].colInc,
                src->plane[2].rowInc, src->plane[2].colInc);
        for (size_t ix = 0; ix < tgt->numPlanes; ++ix) {
            uint8_t *row = const_cast<uint8_t *>(tgt->plane[ix].buffer) + tgt->plane[ix].cropOffs;
            for (size_t y = 0; y < tgt->plane[ix].cropHeight; ++y) {
                uint8_t *col = row;
                ssize_t colInc = tgt->plane[ix].colInc;
                const uint8_t *srcRow = (src->plane[ix].buffer + src->plane[ix].cropOffs
                        + src->plane[ix].rowInc * (y % src->plane[ix].cropHeight));
                for (size_t x = 0; x < tgt->plane[ix].cropWidth; ++x) {
                    *col = srcRow[src->plane[ix].colInc * (x % src->plane[ix].cropWidth)];
                    col += colInc;
                }
                row += tgt->plane[ix].rowInc;
            }
        }
    }
}
