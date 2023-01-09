package com.test;

// uk.tensorzoom_2018-01-18.apk
// 2023-01-07 23:06:23
public class Hunter_uk_tensorzoom_20180118_apk {
    // framework instance
    org.tensorflow.contrib.android.TensorFlowInferenceInterface tf0;
    // bitmap input
    android.graphics.Bitmap picture;
    // context
    android.content.Context context;
    // results
    java.util.HashMap results;

    public Hunter_uk_tensorzoom_20180118_apk(android.content.Context context) {
        try {
            // get context
            this.context = context;

            // init results hashmap
            this.results = new java.util.HashMap();

            // init and load model
            java.io.InputStream model_is = context.getAssets().open("uk.tensorzoom_2018-01-18/5-g2-face-fix.pb");
            tf0 = new org.tensorflow.contrib.android.TensorFlowInferenceInterface(model_is);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void go() {
        try {
            a958161465();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPicture(android.graphics.Bitmap picture) {
        this.picture = picture;
    }

    public android.graphics.Bitmap getPicture() {
        return picture;
    }

    public java.util.HashMap getResults() {
        return results;
    }

    void a958161465() throws Exception {
        int i2 = 0;
        int[] r13;
        int i1 = 0;
        int i3 = 0;
        android.graphics.Bitmap.Config r15;
        int i0 = 0;

        i3 = a751054575();
        i2 = a751054575();
        i0 = i3 * i2;
        r13 = new int[i0];
        picture.getPixels(r13, 0, i3, 0, 0, i3, i2);
        i0 = i2 * 4;
        i1 = i3 * 4;
        r13 = a908585290(r13, i2, i3, i0, i1);
        i0 = i3 * 4;
        i1 = i2 * 4;
        r15 = android.graphics.Bitmap.Config.ARGB_8888;
        picture = android.graphics.Bitmap.createBitmap(i0, i1, r15);
        i0 = i3 * 4;
        i3 = i3 * 4;
        i2 = i2 * 4;
        picture.setPixels(r13, 0, i0, 0, 0, i3, i2);
        return;


//         $r16 = staticinvoke <android.graphics.Bitmap: android.graphics.Bitmap createBitmap(int,int,android.graphics.Bitmap$Config)>($i0, $i1, $r15) (1366354036) 
//         $i0 = $i3 * 4 (1956599961) 
//         $r13 = virtualinvoke $r14.<uk.tensorzoom.c: int[] a(int[],int,int,int,int)>($r13, $i2, $i3, $i0, $i1) (181270977) 
//         $i3 = staticinvoke <uk.tensorzoom.b: int a()>() (1938999690) 
//         $i2 = staticinvoke <uk.tensorzoom.b: int a()>() (996251727) 
//         $i3 = $i3 * 4 (1471811476) 
//         virtualinvoke $r16.<android.graphics.Bitmap: void setPixels(int[],int,int,int,int,int,int)>($r13, 0, $i0, 0, 0, $i3, $i2) (1373861820) 
//         $i0 = $i2 * 4 (699849172) 
//         $i1 = $i3 * 4 (495739917) 
//         $i2 = $i2 * 4 (1829518741) 
//         virtualinvoke $r11.<android.graphics.Bitmap: void getPixels(int[],int,int,int,int,int,int)>($r13, 0, $i3, 0, 0, $i3, $i2) (1770795027) 
//         $i0 = $i3 * $i2 (639192510) 
//         $r13 = newarray (int)[$i0] (300288558) 
//         $r15 = <android.graphics.Bitmap$Config: android.graphics.Bitmap$Config ARGB_8888> (873860294) 
//         $i1 = $i2 * 4 (1990777976) 
//         $i0 = $i3 * 4 (1797924034) 
    }

/**<uk.tensorzoom.browser.c: android.graphics.Bitmap a(android.graphics.Rect)>
*		r0 := @this: uk.tensorzoom.browser.c (1471606371) 

*		$r1 := @parameter0: android.graphics.Rect (290152733) 

*		$r4 = r0.<uk.tensorzoom.browser.c: java.io.File f> (1030832878) 

*		$z0 = virtualinvoke $r4.<java.io.File: boolean exists()>() (811098949) 

*		if $z0 != 0 goto $r4 = r0.<uk.tensorzoom.browser.c: java.io.File f> (841521214) 

*		$r5 = r0.<uk.tensorzoom.browser.c: uk.tensorzoom.browser.a d> (737673597) 

*		$r6 = r0.<uk.tensorzoom.browser.c: android.graphics.Rect e> (1037552187) 

*		virtualinvoke $r5.<uk.tensorzoom.browser.a: android.graphics.Bitmap a(android.graphics.Rect)>($r6) (764795960) 

*		$r4 = r0.<uk.tensorzoom.browser.c: java.io.File f> (1370410869) 

*		$z0 = virtualinvoke $r4.<java.io.File: boolean exists()>() (1386959209) 

*		if $z0 != 0 goto $r8 = specialinvoke r0.<uk.tensorzoom.browser.c: java.lang.String e(android.graphics.Rect)>($r1) (471445774) 

*		$r7 = new java.lang.RuntimeException (1578345008) 

*		specialinvoke $r7.<java.lang.RuntimeException: void <init>(java.lang.String)>("file not found even after parent getBitmap called") (1716497838) 

*		throw $r7 (388482950) 

*		$r8 = specialinvoke r0.<uk.tensorzoom.browser.c: java.lang.String e(android.graphics.Rect)>($r1) (1416033648) 

*		$r9 = r0.<uk.tensorzoom.browser.c: android.util.LruCache a> (1826148491) 

*		$r10 = virtualinvoke $r9.<android.util.LruCache: java.lang.Object get(java.lang.Object)>($r8) (1331859637) 

*		$r11 = (android.graphics.Bitmap) $r10 (1908933366) 

*		if $r11 == null goto $r4 = specialinvoke r0.<uk.tensorzoom.browser.c: java.io.File d(android.graphics.Rect)>($r1) (303014268) 

*		$z0 = virtualinvoke $r11.<android.graphics.Bitmap: boolean isRecycled()>() (370559279) 

*		if $z0 != 0 goto $r9 = r0.<uk.tensorzoom.browser.c: android.util.LruCache a> (1555281920) 

*		return $r11 (323771003) 

*		$r9 = r0.<uk.tensorzoom.browser.c: android.util.LruCache a> (1183496348) 

*		virtualinvoke $r9.<android.util.LruCache: java.lang.Object remove(java.lang.Object)>($r8) (77866720) 

*		$r4 = specialinvoke r0.<uk.tensorzoom.browser.c: java.io.File d(android.graphics.Rect)>($r1) (179169070) 

*		$z0 = virtualinvoke $r4.<java.io.File: boolean exists()>() (1035149700) 

*		if $z0 == 0 goto $r12 = r0.<uk.tensorzoom.browser.c: java.io.File f> (707539648) 

*		$r3 = virtualinvoke $r4.<java.io.File: java.lang.String getAbsolutePath()>() (1671389508) 

*		$r11 = staticinvoke <android.graphics.BitmapFactory: android.graphics.Bitmap decodeFile(java.lang.String)>($r3) (706607574) 

*		$r9 = r0.<uk.tensorzoom.browser.c: android.util.LruCache a> (1399383075) 

*		virtualinvoke $r9.<android.util.LruCache: java.lang.Object put(java.lang.Object,java.lang.Object)>($r8, $r11) (845454944) 

*		return $r11 (516187750) 

*		$r12 = r0.<uk.tensorzoom.browser.c: java.io.File f> (1922790211) 

*		$r3 = virtualinvoke $r12.<java.io.File: java.lang.String getAbsolutePath()>() (1679455470) 

*		virtualinvoke r0.<uk.tensorzoom.browser.c: void a(java.lang.String)>($r3) (884547969) 

*		$r11 = specialinvoke r0.<uk.tensorzoom.browser.a: android.graphics.Bitmap a(android.graphics.Rect)>($r1) (62099196) 

*		$i2 = virtualinvoke $r11.<android.graphics.Bitmap: int getWidth()>() (1993178858) 

*		$i3 = $i2 (184941723) 

*		$i0 = staticinvoke <uk.tensorzoom.b: int a()>() (341722985) 

*		if $i2 <= $i0 goto $i0 = virtualinvoke $r11.<android.graphics.Bitmap: int getHeight()>() (2070701898) 

*		$i3 = staticinvoke <uk.tensorzoom.b: int a()>() (1938999690) 

*		$i0 = virtualinvoke $r11.<android.graphics.Bitmap: int getHeight()>() (1154110226) 

*		$i2 = $i0 (1444557818) 

*		$i1 = staticinvoke <uk.tensorzoom.b: int a()>() (1318712230) 

*		if $i0 <= $i1 goto $r11 = staticinvoke <android.graphics.Bitmap: android.graphics.Bitmap createScaledBitmap(android.graphics.Bitmap,int,int,boolean)>($r11, $i3, $i2, 0) (344407322) 

*		$i2 = staticinvoke <uk.tensorzoom.b: int a()>() (996251727) 

*		$r11 = staticinvoke <android.graphics.Bitmap: android.graphics.Bitmap createScaledBitmap(android.graphics.Bitmap,int,int,boolean)>($r11, $i3, $i2, 0) (154321416) 

*		$i0 = $i3 * $i2 (639192510) 

*		$r13 = newarray (int)[$i0] (300288558) 

*		virtualinvoke $r11.<android.graphics.Bitmap: void getPixels(int[],int,int,int,int,int,int)>($r13, 0, $i3, 0, 0, $i3, $i2) (1770795027) 

*		virtualinvoke $r11.<android.graphics.Bitmap: void recycle()>() (1812765180) 

*		$z0 = r0.<uk.tensorzoom.browser.c: boolean k> (2071745805) 

*		if $z0 == 0 goto $r19 = r0.<uk.tensorzoom.browser.c: uk.tensorstyle.TensorRunner b> (1429599282) 

*		$r14 = r0.<uk.tensorzoom.browser.c: uk.tensorzoom.c c> (821765883) 

*		$i0 = $i2 * 4 (699849172) 

*		$i1 = $i3 * 4 (495739917) 

*		$r13 = virtualinvoke $r14.<uk.tensorzoom.c: int[] a(int[],int,int,int,int)>($r13, $i2, $i3, $i0, $i1) (181270977) 

*		$i0 = $i3 * 4 (1956599961) 

*		$i1 = $i2 * 4 (1990777976) 

*		$r15 = <android.graphics.Bitmap$Config: android.graphics.Bitmap$Config ARGB_8888> (873860294) 

*		$r16 = staticinvoke <android.graphics.Bitmap: android.graphics.Bitmap createBitmap(int,int,android.graphics.Bitmap$Config)>($i0, $i1, $r15) (1366354036) 

*		$r11 = $r16 (1596275027) 

*		$i0 = $i3 * 4 (1797924034) 

*		$i3 = $i3 * 4 (1471811476) 

*		$i2 = $i2 * 4 (1829518741) 

*		virtualinvoke $r16.<android.graphics.Bitmap: void setPixels(int[],int,int,int,int,int,int)>($r13, 0, $i0, 0, 0, $i3, $i2) (1373861820) 

*		virtualinvoke $r4.<java.io.File: boolean createNewFile()>() (1453458619) 

*		$r17 = new java.io.FileOutputStream (1523481455) 

*		specialinvoke $r17.<java.io.FileOutputStream: void <init>(java.io.File)>($r4) (1030649758) 

*		$r18 = <android.graphics.Bitmap$CompressFormat: android.graphics.Bitmap$CompressFormat JPEG> (2075081556) 

*		virtualinvoke $r16.<android.graphics.Bitmap: boolean compress(android.graphics.Bitmap$CompressFormat,int,java.io.OutputStream)>($r18, 100, $r17) (1892262053) 

*		virtualinvoke $r17.<java.io.FileOutputStream: void close()>() (1054206885) 

*		goto [?= $r9 = r0.<uk.tensorzoom.browser.c: android.util.LruCache a>] (1008861872) 

*		$r19 = r0.<uk.tensorzoom.browser.c: uk.tensorstyle.TensorRunner b> (1243186403) 

*		$r2 = r0.<uk.tensorzoom.browser.c: android.content.Context j> (2006673556) 

*		$r3 = r0.<uk.tensorzoom.browser.c: java.lang.String i> (1082852155) 

*		$i0 = $i2 * 4 (492849930) 

*		$i1 = $i3 * 4 (831737850) 

*		$r13 = virtualinvoke $r19.<uk.tensorstyle.TensorRunner: int[] a(android.content.Context,java.lang.String,int[],int,int,int,int)>($r2, $r3, $r13, $i2, $i3, $i0, $i1) (339958650) 

*		goto [?= $i0 = $i3 * 4] (213408068) 
*/

    int[] a1280708176(float[] p0) throws Exception {
        int i3 = 0;
        int[] r2;
        int i4 = 0;
        int i0 = 0;
        int i2 = 0;
        float[] r1;
        int i1 = 0;
        float f0;

        r1 = p0;
        i0 = r1.length;
        i0 = i0 / 3;
        r2 = new int[i0];
        i0 = r2.length;
        i1 = 0;
        while (!(i1 >= i0)) {
            i2 = i1 * 3;
            f0 = r1[i2];
            f0 = f0 * 255.0F;
            i2 = (int)f0;
            i3 = i1 * 3;
            i3 = i3 + 1;
            f0 = r1[i3];
            f0 = f0 * 255.0F;
            i3 = (int)f0;
            i4 = i1 * 3;
            i4 = i4 + 2;
            f0 = r1[i4];
            f0 = f0 * 255.0F;
            i4 = (int)f0;
            if (!(i2 <= 255)) {
                i2 = 255;
            } else {
                if (!(i2 >= 0)) {
                    i2 = 0;
                }
            }
            if (!(i3 <= 255)) {
                i3 = 255;
            } else {
                if (!(i3 >= 0)) {
                    i3 = 0;
                }
            }
            if (!(i4 <= 255)) {
                i4 = 255;
            } else {
                if (!(i4 >= 0)) {
                    i4 = 0;
                }
            }
            i2 = i2 << 16;
            i2 = i2 | -16777216;
            i3 = i3 << 8;
            i2 = i3 | i2;
            i2 = i4 | i2;
            r2[i1] = i2;
            i1 = i1 + 1;
        }
        return r2;


//         $r2[$i1] = $i2 (488912164) 
//         if $i3 >= 0 goto (branch) (1888235403) 
//         $i2 = (int) $f0 (1989357979) 
//         $i1 = $i1 + 1 (249150160) 
//         $i4 = 255 (1928124134) 
//         $f0 = $f0 * 255.0F (1836545405) 
//         if $i4 <= 255 goto (branch) (61714111) 
//         $i3 = (int) $f0 (98835965) 
//         $r1 := @parameter0: float[] (1328321289) 
//         $i4 = $i1 * 3 (422285831) 
//         return $r2 (1617452357) 
//         $i3 = $i3 << 8 (1702057280) 
//         if $i3 <= 255 goto (branch) (562912702) 
//         if $i1 >= $i0 goto return $r2 (1586571691) 
//         $f0 = $r1[$i3] (548904074) 
//         $i4 = $i4 + 2 (1844428754) 
//         $i2 = $i4 | $i2 (188573050) 
//         $f0 = $r1[$i4] (220696173) 
//         goto [?= (branch)] (1727259321) 
//         $i0 = lengthof $r2 (1208309076) 
//         $i2 = 0 (633328612) 
//         $r2 = newarray (int)[$i0] (1427596148) 
//         goto [?= (branch)] (1856964804) 
//         if $i2 <= 255 goto (branch) (2111779248) 
//         $i3 = 255 (913679880) 
//         $i0 = $i0 / 3 (366900896) 
//         $i2 = $i2 << 16 (2048785250) 
//         $f0 = $f0 * 255.0F (84718726) 
//         $f0 = $r1[$i2] (1224331108) 
//         $i1 = 0 (449076334) 
//         goto [?= $i2 = $i2 << 16] (1499508184) 
//         $i3 = $i3 + 1 (10492187) 
//         if $i2 >= 0 goto (branch) (1386122528) 
//         $i2 = $i3 | $i2 (518937390) 
//         $f0 = $f0 * 255.0F (811735719) 
//         $i2 = 255 (804719665) 
//         $i2 = $i2 | -16777216 (1908209271) 
//         $i3 = $i1 * 3 (1880576706) 
//         $i4 = (int) $f0 (2113291822) 
//         goto [?= (branch)] (220631803) 
//         $i0 = lengthof $r1 (609147817) 
//         $i3 = 0 (1457343287) 
//         r0 := @this: uk.tensorzoom.c (1330959290) 
//         $i4 = 0 (1700316855) 
//         $i2 = $i1 * 3 (304845534) 
//         if $i4 >= 0 goto $i2 = $i2 << 16 (1949913036) 
    }

/**<uk.tensorzoom.c: int[] a(float[])>
*		r0 := @this: uk.tensorzoom.c (1330959290) 

*		$r1 := @parameter0: float[] (1328321289) 

*		$i0 = lengthof $r1 (609147817) 

*		$i0 = $i0 / 3 (366900896) 

*		$r2 = newarray (int)[$i0] (1427596148) 

*		$i0 = lengthof $r2 (1208309076) 

*		$i1 = 0 (449076334) 

*		if $i1 >= $i0 goto return $r2 (1586571691) 

*		$i2 = $i1 * 3 (304845534) 

*		$f0 = $r1[$i2] (1224331108) 

*		$f0 = $f0 * 255.0F (811735719) 

*		$i2 = (int) $f0 (1989357979) 

*		$i3 = $i1 * 3 (1880576706) 

*		$i3 = $i3 + 1 (10492187) 

*		$f0 = $r1[$i3] (548904074) 

*		$f0 = $f0 * 255.0F (84718726) 

*		$i3 = (int) $f0 (98835965) 

*		$i4 = $i1 * 3 (422285831) 

*		$i4 = $i4 + 2 (1844428754) 

*		$f0 = $r1[$i4] (220696173) 

*		$f0 = $f0 * 255.0F (1836545405) 

*		$i4 = (int) $f0 (2113291822) 

*		if $i2 <= 255 goto (branch) (2111779248) 

*		$i2 = 255 (804719665) 

*		if $i3 <= 255 goto (branch) (562912702) 

*		$i3 = 255 (913679880) 

*		if $i4 <= 255 goto (branch) (61714111) 

*		$i4 = 255 (1928124134) 

*		$i2 = $i2 << 16 (2048785250) 

*		$i2 = $i2 | -16777216 (1908209271) 

*		$i3 = $i3 << 8 (1702057280) 

*		$i2 = $i3 | $i2 (518937390) 

*		$i2 = $i4 | $i2 (188573050) 

*		$r2[$i1] = $i2 (488912164) 

*		$i1 = $i1 + 1 (249150160) 

*		goto [?= (branch)] (1856964804) 

*		if $i2 >= 0 goto (branch) (1386122528) 

*		$i2 = 0 (633328612) 

*		goto [?= (branch)] (1727259321) 

*		if $i3 >= 0 goto (branch) (1888235403) 

*		$i3 = 0 (1457343287) 

*		goto [?= (branch)] (220631803) 

*		if $i4 >= 0 goto $i2 = $i2 << 16 (1949913036) 

*		$i4 = 0 (1700316855) 

*		goto [?= $i2 = $i2 << 16] (1499508184) 

*		return $r2 (1617452357) 
*/

    float[] a1151362551(int[] p0) throws Exception {
        int i1 = 0;
        int i0 = 0;
        boolean z0 = false;
        int[] r1;
        java.lang.Object r5;
        java.util.ListIterator listIterator;
        float[] r2;
        int i2 = 0;
        int i3 = 0;
        float f0;
        java.util.List list;
        java.lang.Number r7;

        r1 = p0;
        i0 = r1.length;
        i0 = i0 * 3;
        r2 = new float[i0];
        list = java.util.Arrays.stream(r1).boxed().collect(java.util.stream.Collectors.toList());
        listIterator = list.listIterator();
        z0 = listIterator.hasNext();
        while (!(z0 == false)) {
            r5 = listIterator.next();
            i1 = listIterator.nextIndex() - 1;
            r7 = (java.lang.Number)r5;
            i0 = r7.intValue();
            i2 = i1 * 3;
            i3 = i0 >> 16;
            i3 = i3 & 255;
            f0 = (float)i3;
            f0 = f0 / 255.0F;
            r2[i2] = f0;
            i2 = i1 * 3;
            i2 = i2 + 1;
            i3 = i0 >> 8;
            i3 = i3 & 255;
            f0 = (float)i3;
            f0 = f0 / 255.0F;
            r2[i2] = f0;
            i1 = i1 * 3;
            i1 = i1 + 2;
            i0 = i0 & 255;
            f0 = (float)i0;
            f0 = f0 / 255.0F;
            r2[i1] = f0;
            z0 = listIterator.hasNext();
        }
        return r2;


//         $i1 = $i1 + 2 (1462253358) 
//         $i2 = $i1 * 3 (1863122699) 
//         $f0 = $f0 / 255.0F (323392577) 
//         $z0 = interfaceinvoke listIterator.<java.util.Iterator: boolean hasNext()>() (1089995745) 
//         $i3 = $i0 >> 16 (1119442584) 
//         return $r2 (499297508) 
//         if $z0 == 0 goto return $r2 (236144184) 
//         $i1 = $i1 * 3 (659943527) 
//         $i1 = interfaceinvoke listIterator.<java.util.ListIterator: int nextIndex()>() (1471145109) 
//         $r2[$i1] = $f0 (1138691981) 
//         $f0 = (float) $i0 (1560725072) 
//         $r5 = interfaceinvoke listIterator.<java.util.Iterator: java.lang.Object next()>() (647751876) 
//         $r2[$i2] = $f0 (457383207) 
//         $i3 = $i3 & 255 (762001934) 
//         $i0 = lengthof $r1 (1691469481) 
//         $r2 = newarray (float)[$i0] (1910269023) 
//         $f0 = $f0 / 255.0F (1440269151) 
//         $r7 = (java.lang.Number) $r5 (1775512142) 
//         $f0 = (float) $i3 (524008359) 
//         $f0 = $f0 / 255.0F (1757501283) 
//         $i2 = $i1 * 3 (1037166194) 
//         $r2[$i2] = $f0 (1026207367) 
//         $i3 = $i3 & 255 (155350265) 
//         $f0 = (float) $i3 (29243005) 
//         $i0 = $i0 * 3 (65596206) 
//         list = staticinvoke <kotlin.collections.ArraysKt: java.lang.Iterable withIndex(int[])>($r1) (1928305700) 
//         $i0 = $i0 & 255 (892344551) 
//         $i3 = $i0 >> 8 (174848694) 
//         listIterator = interfaceinvoke list.<java.util.List: java.util.ListIterator listIterator()>() (1544864247) 
//         $r1 := @parameter0: int[] (2113772318) 
//         $i2 = $i2 + 1 (757635022) 
//         $i0 = virtualinvoke $r7.<java.lang.Number: int intValue()>() (410381459) 
    }

/**<uk.tensorzoom.c: float[] a(int[])>
*		r0 := @this: uk.tensorzoom.c (1239429365) 

*		$r1 := @parameter0: int[] (2113772318) 

*		$i0 = lengthof $r1 (1691469481) 

*		$i0 = $i0 * 3 (65596206) 

*		$r2 = newarray (float)[$i0] (1910269023) 

*		list = staticinvoke <kotlin.collections.ArraysKt: java.lang.Iterable withIndex(int[])>($r1) (1928305700) 

*		listIterator = interfaceinvoke list.<java.util.List: java.util.ListIterator listIterator()>() (1544864247) 

*		$z0 = interfaceinvoke listIterator.<java.util.Iterator: boolean hasNext()>() (1089995745) 

*		if $z0 == 0 goto return $r2 (236144184) 

*		$r5 = interfaceinvoke listIterator.<java.util.Iterator: java.lang.Object next()>() (647751876) 

*		$i1 = interfaceinvoke listIterator.<java.util.ListIterator: int nextIndex()>() (1471145109) 

*		$r7 = (java.lang.Number) $r5 (1775512142) 

*		$i0 = virtualinvoke $r7.<java.lang.Number: int intValue()>() (410381459) 

*		$i2 = $i1 * 3 (1037166194) 

*		$i3 = $i0 >> 16 (1119442584) 

*		$i3 = $i3 & 255 (762001934) 

*		$f0 = (float) $i3 (524008359) 

*		$f0 = $f0 / 255.0F (323392577) 

*		$r2[$i2] = $f0 (457383207) 

*		$i2 = $i1 * 3 (1863122699) 

*		$i2 = $i2 + 1 (757635022) 

*		$i3 = $i0 >> 8 (174848694) 

*		$i3 = $i3 & 255 (155350265) 

*		$f0 = (float) $i3 (29243005) 

*		$f0 = $f0 / 255.0F (1757501283) 

*		$r2[$i2] = $f0 (1026207367) 

*		$i1 = $i1 * 3 (659943527) 

*		$i1 = $i1 + 2 (1462253358) 

*		$i0 = $i0 & 255 (892344551) 

*		$f0 = (float) $i0 (1560725072) 

*		$f0 = $f0 / 255.0F (1440269151) 

*		$r2[$i1] = $f0 (1138691981) 

*		goto [?= $z0 = interfaceinvoke listIterator.<java.util.Iterator: boolean hasNext()>()] (1981665329) 

*		return $r2 (499297508) 
*/

    int a751054575() throws Exception {

        return 64;


//         return 64 (97475595) 
    }

/**<uk.tensorzoom.b: int a()>
*		$r0 = <uk.tensorzoom.MainApp: uk.tensorzoom.MainApp a> (596430157) 

*		$z0 = staticinvoke <uk.tensorzoom.b: boolean f(android.content.Context)>($r0) (1539283819) 

*		if $z0 == 0 goto return 64 (1597585019) 

*		$r0 = <uk.tensorzoom.MainApp: uk.tensorzoom.MainApp a> (93506237) 

*		$i0 = staticinvoke <uk.tensorzoom.b: int g(android.content.Context)>($r0) (733719578) 

*		return $i0 (1858506058) 

*		return 64 (97475595) 
*/

    int[] a908585290(int[] p0, int p1, int p2, int p3, int p4) throws Exception {
        int i1 = 0;
        float[] r2;
        long l4 = 0;
        int i0 = 0;
        int i2 = 0;
        long[] r6;
        java.lang.String[] r7;
        int i3 = 0;
        float[] r3;
        int[] r1;

        r1 = p0;
        i0 = p1;
        i1 = p2;
        i2 = p3;
        i3 = p4;
        r2 = a1151362551(r1);
        i2 = i2 * i3;
        i2 = i2 * 3;
        r3 = new float[i2];
        r1 = new int[3];
        r1[0] = i0;
        r1[1] = i1;
        r1[2] = 3;
        r6 = new long[1];
        r6[0] = 3L;
        tf0.feed("input_shape", r1, r6);
        r6 = new long[3];
        l4 = (long)i0;
        r6[0] = l4;
        l4 = (long)i1;
        r6[1] = l4;
        r6[2] = 3L;
        tf0.feed("input", r2, r6);
        r7 = new java.lang.String[1];
        r7[0] = "output";
        tf0.run(r7, false);
        tf0.fetch("output", r3);
        r1 = a1280708176(r3);
        return r1;


//         $r6 = newarray (long)[1] (1531874396) 
//         $r1[0] = $i0 (49904621) 
//         $l4 = (long) $i1 (1804662448) 
//         $r6[2] = 3L (2077721077) 
//         $r7 = newarray (java.lang.String)[1] (1949579294) 
//         $i0 := @parameter1: int (761029741) 
//         $r3 = newarray (float)[$i2] (265590423) 
//         $i3 := @parameter4: int (1721938686) 
//         $r2 = specialinvoke r0.<uk.tensorzoom.c: float[] a(int[])>($r1) (877845811) 
//         virtualinvoke $r5.<org.tensorflow.contrib.android.a: void a(java.lang.String,float[])>("output", $r3) (1477504103) 
//         virtualinvoke $r5.<org.tensorflow.contrib.android.a: void a(java.lang.String,float[],long[])>("input", $r2, $r6) (165165988) 
//         $r6[1] = $l4 (83804275) 
//         $r6[0] = 3L (1657595203) 
//         $i2 = $i2 * 3 (768441688) 
//         $l4 = (long) $i0 (1142252171) 
//         $r1[2] = 3 (1366374869) 
//         $r6[0] = $l4 (1759134591) 
//         $i2 := @parameter3: int (1099047460) 
//         $i2 = $i2 * $i3 (1277966743) 
//         virtualinvoke $r5.<org.tensorflow.contrib.android.a: void a(java.lang.String[],boolean)>($r7, 0) (839329209) 
//         $r1 = newarray (int)[3] (963087543) 
//         $r1 := @parameter0: int[] (568399667) 
//         return $r1 (164126747) 
//         $r7[0] = "output" (775514845) 
//         virtualinvoke $r5.<org.tensorflow.contrib.android.a: void a(java.lang.String,int[],long[])>("input_shape", $r1, $r6) (1305458986) 
//         $r1[1] = $i1 (1366105756) 
//         $i1 := @parameter2: int (1648963515) 
//         $r6 = newarray (long)[3] (1503295587) 
//         $r1 = specialinvoke r0.<uk.tensorzoom.c: int[] a(float[])>($r3) (1870699647) 
    }

/**<uk.tensorzoom.c: int[] a(int[],int,int,int,int)>
*		r0 := @this: uk.tensorzoom.c (1667409865) 

*		$r1 := @parameter0: int[] (568399667) 

*		$i0 := @parameter1: int (761029741) 

*		$i1 := @parameter2: int (1648963515) 

*		$i2 := @parameter3: int (1099047460) 

*		$i3 := @parameter4: int (1721938686) 

*		staticinvoke <kotlin.jvm.internal.Intrinsics: void checkParameterIsNotNull(java.lang.Object,java.lang.String)>($r1, "pixels") (581465351) 

*		$r2 = specialinvoke r0.<uk.tensorzoom.c: float[] a(int[])>($r1) (877845811) 

*		$i2 = $i2 * $i3 (1277966743) 

*		$i2 = $i2 * 3 (768441688) 

*		$r3 = newarray (float)[$i2] (265590423) 

*		$r4 = specialinvoke r0.<uk.tensorzoom.c: org.tensorflow.contrib.android.a b()>() (1644381465) 

*		entermonitor $r4 (154123091) 

*		$r5 = specialinvoke r0.<uk.tensorzoom.c: org.tensorflow.contrib.android.a b()>() (1686438222) 

*		$r1 = newarray (int)[3] (963087543) 

*		$r1[0] = $i0 (49904621) 

*		$r1[1] = $i1 (1366105756) 

*		$r1[2] = 3 (1366374869) 

*		$r6 = newarray (long)[1] (1531874396) 

*		$r6[0] = 3L (1657595203) 

*		virtualinvoke $r5.<org.tensorflow.contrib.android.a: void a(java.lang.String,int[],long[])>("input_shape", $r1, $r6) (1305458986) 

*		$r5 = specialinvoke r0.<uk.tensorzoom.c: org.tensorflow.contrib.android.a b()>() (1887837524) 

*		$r6 = newarray (long)[3] (1503295587) 

*		$l4 = (long) $i0 (1142252171) 

*		$r6[0] = $l4 (1759134591) 

*		$l4 = (long) $i1 (1804662448) 

*		$r6[1] = $l4 (83804275) 

*		$r6[2] = 3L (2077721077) 

*		virtualinvoke $r5.<org.tensorflow.contrib.android.a: void a(java.lang.String,float[],long[])>("input", $r2, $r6) (165165988) 

*		$r5 = specialinvoke r0.<uk.tensorzoom.c: org.tensorflow.contrib.android.a b()>() (438244429) 

*		$r7 = newarray (java.lang.String)[1] (1949579294) 

*		$r7[0] = "output" (775514845) 

*		virtualinvoke $r5.<org.tensorflow.contrib.android.a: void a(java.lang.String[],boolean)>($r7, 0) (839329209) 

*		$r5 = specialinvoke r0.<uk.tensorzoom.c: org.tensorflow.contrib.android.a b()>() (251946308) 

*		virtualinvoke $r5.<org.tensorflow.contrib.android.a: void a(java.lang.String,float[])>("output", $r3) (1477504103) 

*		$r8 = <kotlin.Unit: kotlin.Unit INSTANCE> (1791857228) 

*		exitmonitor $r4 (808205769) 

*		$r1 = specialinvoke r0.<uk.tensorzoom.c: int[] a(float[])>($r3) (1870699647) 

*		return $r1 (164126747) 

*		$r9 := @caughtexception (1482413726) 

*		$r10 = r0.<uk.tensorzoom.c: java.lang.ref.WeakReference c> (869522062) 

*		$r11 = virtualinvoke $r10.<java.lang.ref.WeakReference: java.lang.Object get()>() (841079639) 

*		$r12 = (android.content.Context) $r11 (2127194908) 

*		$z0 = $r12 instanceof android.app.Activity (1407240673) 

*		if $z0 == 0 goto $r13 = null (646297172) 

*		goto [?= $r13 = $r12] (1090774711) 

*		$r13 = null (168572368) 

*		$r14 = (android.app.Activity) $r13 (666053894) 

*		if $r14 != null goto $r15 = new uk.tensorzoom.c$b (1624447582) 

*		goto [?= $r17 = (java.lang.Throwable) $r9] (287358145) 

*		$r15 = new uk.tensorzoom.c$b (669392931) 

*		specialinvoke $r15.<uk.tensorzoom.c$b: void <init>(android.content.Context,java.lang.Exception)>($r12, $r9) (1278157754) 

*		$r16 = (java.lang.Runnable) $r15 (1241858682) 

*		virtualinvoke $r14.<android.app.Activity: void runOnUiThread(java.lang.Runnable)>($r16) (1777553126) 

*		$r17 = (java.lang.Throwable) $r9 (738965831) 

*		throw $r17 (668009847) 

*		$r17 := @caughtexception (44628377) 

*		exitmonitor $r4 (1931000074) 

*		throw $r17 (276207423) 

*		$r13 = $r12 (1067700987) 

*		goto [?= $r14 = (android.app.Activity) $r13] (2055372967) 
*/

}
