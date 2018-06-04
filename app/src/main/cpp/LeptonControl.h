//
// Created by Kevin on 16/12/12.
//

#ifndef HYL026_LEPTONCONTROL_H
#define HYL026_LEPTONCONTROL_H

#include "SocketManager.h"
#include "ColorTable.h"
#include "ErrorTable.h"

#define  FIXED_BITS           16
#define  FIXED_ONE            (1 << FIXED_BITS)
#define  PALETTE_BITS   8
#define  PALETTE_SIZE   256//(1 << PALETTE_BITS)
#define  FIXED_FRAC(x)        ((x) & ((1 << FIXED_BITS)-1))

int init();

int search(JNIEnv *env, jobject obj, jmethodID callback);

long openDevice(const char *ip, int port, DEVICE_TYPE deviceType);

long openDeviceByCB(const char *ip, int port, DEVICE_TYPE deviceType,JNIEnv *env, jobject obj, jmethodID callback);

int openThread(P_HANDLE handle);

void setScalingDataByHandle(P_HANDLE handle, short scalingData[]);

double temperatureByGray(P_HANDLE handle, short gray, double e, double s, double value);

//void createConnectListener();

P_HANDLE getPointerByIndex(int index);
//
//P_HANDLE  getPointerByHandle(int handle);

int startDevice(P_HANDLE handle);

int pauseDevice(P_HANDLE handle);

int closeDevice(P_HANDLE handle);

int correctDevice(P_HANDLE handle);

int _14To8(P_HANDLE,short *data, unsigned char *bmpData);

int _14To565(P_HANDLE,short *data, int* bmpData);

int _14To565Toshort(P_HANDLE,short *data, short* bmpData);

void getNextFrame(P_HANDLE handle, short *raw);

void setColorsName(int colorsName);

void setColorName(int colorName);

int getColorName(void);

void addFileHeader(int size);

void addBMPImage_Info_Header(int w, int h);

void init_palette(int index);


#endif //HYL026_LEPTONCONTROL_H
