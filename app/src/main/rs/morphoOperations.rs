#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(com.paperclickers.fiducial)

static uint32_t const PIXEL_COLOR_MASK = 0x01000000;

static uint32_t const MEDIAN_FILTER_MAX_ELEMENT_SIZE = 7;

uint32_t width;
uint32_t height;
uint32_t elementSize;
uint32_t halfElementSize;

rs_allocation currentInput;



void adaptiveThreshold() {
    int a;
    int threshold, sum = 128;
    int s = 30;

    uint column;

    double f = 0.975;

    bool invert = false;

    for (int i = 0; i < height; i++) {

        // ----------------------------------------
        // Process rows back and forth (alternating
        // left-to-right, right-to-left)
        // ----------------------------------------
        //k = (j % 2 == 0) ? 0 : w - 1;
        //k += (j * w);

        if (invert) {
            column = width - 1;
        } else {
            column = 0;
        }

        for (int j = 0; j < width; j++) {

            // ----------------------------------------
            // Calculate pixel intensity (0-255)
            // ----------------------------------------

            a = (rsGetElementAt_uint(currentInput, column, i) & 0xFF000000) >> 24;

            // ----------------------------------------
            // Calculate sum as an approximate sum
            // of the last s pixels
            // ----------------------------------------
            sum += a - (sum / s);

            // ----------------------------------------
            // Factor in sum from the previous row
            // ----------------------------------------

            if (i > 0) {
                threshold = (sum + (rsGetElementAt_uint(currentInput, column, i - 1) & 0xffffff)) / (2 * s);
            } else {
                threshold = sum / s;
            }

            // ----------------------------------------
            // Compare the average sum to current pixel
            // to decide black or white
            // ----------------------------------------

            a = (a < threshold * f) ? 0 : 1;

            // ----------------------------------------
            // Repack pixel data with binary data in
            // the alpha channel, and the running sum
            // for this pixel in the RGB channels
            // ----------------------------------------

            rsSetElementAt_uint (currentInput, ((a << 24) + (sum & 0xffffff)), column, i);

            if (invert) {
                column--;
            } else {
                column++;
            }
        }

        invert = !invert;
    }
}



static void quickSort(uint pixels[], int size, int begin, int end) {

    if (size > 1) {
        uint pivot = pixels[begin + size / 2];
        uint tmp;

        int left  = begin;
        int right = end;

        while (left <= right) {

            while (pixels[left] < pivot) {
                left++;
            }

            while (pixels[right] > pivot) {
                right--;
            }

            if (left <= right) {
                tmp = pixels[left];

                pixels[left]  = pixels[right];
                pixels[right] = tmp;

                left++;
                right--;
            }
        }

        uint size1 = right - begin + 1;
        uint size2 = end - left + 1;

        if (size1 > 1) {
            quickSort(pixels, size1, begin, right);
        }

        if (size2 > 1) {
            quickSort(pixels, size2, left, end);
        }
    }
}




//
// Kernels
//

uint RS_KERNEL dilation(uint in, uint32_t x, uint32_t y) {

    bool hasHit = false;

    int32_t startHeight = y - halfElementSize;
    int32_t startWidth  = x - halfElementSize;

    int32_t totalHeight = elementSize;
    int32_t totalWidth  = elementSize;

    uint element;

    uint out;


    for (uint32_t i = 0; i < totalHeight; i++) {
        for (uint32_t j = 0; j < totalWidth; j++) {
            element = rsGetElementAt_uint(currentInput, startWidth + j, startHeight + i);

            if ((element & PIXEL_COLOR_MASK) == 0) {

                hasHit = true;

                break;
            }
        }
    }

    if (hasHit) {
        out = 0;
    } else {
        out = PIXEL_COLOR_MASK;
    }

    return out;
}



uint RS_KERNEL erosion(uint in, uint32_t x, uint32_t y) {

    bool hasFit = true;

    int32_t startHeight = y - halfElementSize;
    int32_t startWidth  = x - halfElementSize;

    int32_t totalHeight = elementSize;
    int32_t totalWidth  = elementSize;

    uint element;

    uint out;


    for (uint32_t i = 0; i < totalHeight; i++) {
        for (uint32_t j = 0; j < totalWidth; j++) {
            element = rsGetElementAt_uint(currentInput, startWidth + j, startHeight + i);

            if ((element & PIXEL_COLOR_MASK) != 0) {

                hasFit = false;

                break;
            }
        }
    }

    if (hasFit) {
        out = 0;
    } else {
        out = PIXEL_COLOR_MASK;
    }

    return out;
}



uint RS_KERNEL median(uint in, uint32_t x, uint32_t y) {

    bool hasHit = false;

    int32_t startHeight = y - halfElementSize;
    int32_t startWidth  = x - halfElementSize;

    int32_t totalHeight = elementSize;
    int32_t totalWidth  = elementSize;

    uint element;
    uint pixels[MEDIAN_FILTER_MAX_ELEMENT_SIZE * MEDIAN_FILTER_MAX_ELEMENT_SIZE];
    uint out;

    uchar pixelsCount = 0;

    for (uint32_t i = 0; i < totalHeight; i++) {
        for (uint32_t j = 0; j < totalWidth; j++) {
            element = rsGetElementAt_uint(currentInput, startWidth + j, startHeight + i);

            pixels[pixelsCount++] = element;
        }
    }

    quickSort(pixels, elementSize * elementSize, 0, (elementSize * elementSize) - 1);

    out = pixels[(elementSize * elementSize - 1) / 2];

    return out;
}
