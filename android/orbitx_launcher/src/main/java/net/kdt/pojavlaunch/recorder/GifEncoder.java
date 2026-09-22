package net.kdt.pojavlaunch.recorder;

import android.graphics.Bitmap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * OrbitX — minimal GIF89a encoder.
 *
 * <p>Android ships no GIF <i>encoder</i> (only a decoder), and OrbitX avoids
 * pulling in a heavyweight third-party encoder just for the optional GIF export
 * path. This writer produces an animated, looping GIF89a using a 6×7×6
 * colour cube (252 palette entries) and the standard LZW compression required
 * by the format.</p>
 *
 * <p>Frames are expected to already be scaled to the requested size.</p>
 */
final class GifEncoder {

    private final OutputStream mOut;
    private final int mWidth;
    private final int mHeight;
    private final int mDelayCs;
    private final byte[] mPalette = new byte[256 * 3];

    private boolean mHeaderWritten = false;

    GifEncoder(OutputStream out, int width, int height, int delayCs, int loopCount) {
        mOut = out;
        mWidth = width;
        mHeight = height;
        mDelayCs = Math.max(2, delayCs);
        buildPalette();
        mHeaderLoopCount = loopCount;
    }

    private final int mHeaderLoopCount;

    private void buildPalette() {
        int i = 0;
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 7; g++) {
                for (int b = 0; b < 6; b++) {
                    mPalette[i++] = (byte) (r * 51);
                    mPalette[i++] = (byte) (g * 42);
                    mPalette[i++] = (byte) (b * 51);
                }
            }
        }
        // Remaining entries (252..255) stay black.
    }

    private static int quantize(int r, int g, int b) {
        int ri = (r * 5 + 127) / 255;
        int gi = (g * 6 + 126) / 252;
        int bi = (b * 5 + 127) / 255;
        return ri * 42 + gi * 6 + bi;
    }

    private void writeHeaderIfNeeded() throws IOException {
        if (mHeaderWritten) return;
        mHeaderWritten = true;
        mOut.write('G');
        mOut.write('I');
        mOut.write('F');
        mOut.write('8');
        mOut.write('9');
        mOut.write('a');
        writeShort(mWidth);
        writeShort(mHeight);
        mOut.write(0xF7);   // global colour table, 256 entries, 8-bit
        mOut.write(0);      // background colour index
        mOut.write(0);      // pixel aspect ratio
        mOut.write(mPalette);
        // NETSCAPE2.0 looping extension
        mOut.write(0x21);
        mOut.write(0xFF);
        mOut.write(0x0B);
        mOut.write("NETSCAPE2.0".getBytes("US-ASCII"));
        mOut.write(0x03);
        mOut.write(0x01);
        writeShort(mHeaderLoopCount);
        mOut.write(0x00);
    }

    void addFrame(Bitmap bitmap) throws IOException {
        writeHeaderIfNeeded();

        int w = mWidth;
        int h = mHeight;
        byte[] indices = new byte[w * h];
        int[] row = new int[w];
        int p = 0;
        for (int y = 0; y < h; y++) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                int c = row[x];
                indices[p++] = (byte) quantize(
                        (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            }
        }

        // Graphic control extension
        mOut.write(0x21);
        mOut.write(0xF9);
        mOut.write(0x04);
        mOut.write(0x04);   // disposal: restore to background, no transparency
        writeShort(mDelayCs);
        mOut.write(0x00);   // transparent index (unused)
        mOut.write(0x00);

        // Image descriptor
        mOut.write(0x2C);
        writeShort(0);
        writeShort(0);
        writeShort(w);
        writeShort(h);
        mOut.write(0x00);   // no local colour table, not interlaced

        lzwEncode(indices);
    }

    void finish() throws IOException {
        writeHeaderIfNeeded();
        mOut.write(0x3B);   // trailer
        mOut.flush();
    }

    private void writeShort(int value) throws IOException {
        mOut.write(value & 0xFF);
        mOut.write((value >> 8) & 0xFF);
    }

    private void lzwEncode(byte[] indices) throws IOException {
        final int clearCode = 256;
        final int endCode = 257;

        mOut.write(8);  // LZW minimum code size
        BlockWriter writer = new BlockWriter(mOut);
        int[] dict = new int[4096];
        Arrays.fill(dict, -1);

        int codeSize = 9;
        int next = 258;
        writer.writeBits(clearCode, codeSize);

        int prefix = indices[0] & 0xFF;
        for (int i = 1; i < indices.length; i++) {
            int pixel = indices[i] & 0xFF;
            int key = (prefix << 8) | pixel;
            if (dict[key] != -1) {
                prefix = dict[key];
                continue;
            }
            writer.writeBits(prefix, codeSize);
            if (next < 4096) {
                dict[key] = next;
                next++;
                if (next == (1 << codeSize) && codeSize < 12) codeSize++;
            } else {
                writer.writeBits(clearCode, codeSize);
                Arrays.fill(dict, -1);
                codeSize = 9;
                next = 258;
            }
            prefix = pixel;
        }
        writer.writeBits(prefix, codeSize);
        writer.writeBits(endCode, codeSize);
        writer.flush();
    }

    /** Packs bit codes LSB-first into GIF data sub-blocks of at most 255 bytes. */
    private static final class BlockWriter {
        private final OutputStream mOut;
        private final byte[] mBlock = new byte[255];
        private int mBlockLen = 0;
        private int mBitBuffer = 0;
        private int mBitCount = 0;

        BlockWriter(OutputStream out) {
            mOut = out;
        }

        void writeBits(int code, int size) throws IOException {
            mBitBuffer |= code << mBitCount;
            mBitCount += size;
            while (mBitCount >= 8) {
                emit((byte) (mBitBuffer & 0xFF));
                mBitBuffer >>>= 8;
                mBitCount -= 8;
            }
        }

        private void emit(byte b) throws IOException {
            mBlock[mBlockLen++] = b;
            if (mBlockLen == 255) flushBlock();
        }

        private void flushBlock() throws IOException {
            if (mBlockLen > 0) {
                mOut.write(mBlockLen);
                mOut.write(mBlock, 0, mBlockLen);
                mBlockLen = 0;
            }
        }

        void flush() throws IOException {
            if (mBitCount > 0) {
                emit((byte) (mBitBuffer & 0xFF));
                mBitBuffer = 0;
                mBitCount = 0;
            }
            flushBlock();
            mOut.write(0x00);   // block terminator
        }
    }
}
