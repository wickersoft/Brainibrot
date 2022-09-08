#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <math.h>
#include <pthread.h>
#include <time.h>

// Output dimensions
const uint64_t HEIGHT = 29300;
const uint64_t WIDTH = (HEIGHT * 5L) / 2L;

// Other parameters
#define ORDER 8      // Change the shape by drawing at the iteration given here
#define THREADS 15   // How fast should the meter spin
#define DPP 256      // Supersampling: Each pixel is equivalent to DPP^2 rendered points. Use 1 for previews

// Color mapping. Fine tune empirically by re-rendering from the raw histogram
#define SCALE_FACTOR 6000
#define GAMMA 0.5
#define X_MIN_CALC -2.0
#define X_MAX_CALC 0.75
#define Y_MIN_CALC -1.1 // automatically goes until 0
#define ITSTEPS_B 40
#define ITSTEPS_G 300
#define ITSTEPS_R 5000

// Coordinates on the complex plane. You shouldn't have to change this
#define X_MIN_PLOT -2
#define X_MAX_PLOT 0.75
#define Y_MIN_PLOT -1.1

#define MASK_WIDTH 19200
#define MASK_HEIGHT 15360

const double Y_MAX_PLOT = Y_MIN_PLOT + (X_MAX_PLOT - X_MIN_PLOT) * HEIGHT / WIDTH;

uint32_t *pixels;
uint32_t brightest[3];
uint8_t mask[(MASK_WIDTH * MASK_HEIGHT) / 8];

const double repixel = ((double) (X_MAX_PLOT - X_MIN_PLOT)) / ((double) WIDTH);
const double impixel = ((double) (Y_MAX_PLOT - Y_MIN_PLOT)) / ((double) HEIGHT);
const double restep = repixel / DPP;
const double imstep = impixel / DPP;

typedef struct {
	uint32_t offset;
	pthread_t thread_id;
} render_thread;

uint8_t check_mask(double re, double im) {
	uint32_t mask_x = ((re + 2.0) / 2.5) * MASK_WIDTH;
	uint32_t mask_y = ((im + 1.0) / 2.0) * MASK_HEIGHT;
	if(mask_x < 0 || mask_x >= MASK_WIDTH) return 1;
	if(mask_y < 0 || mask_y >= MASK_HEIGHT) return 1;
	uint32_t mask_byte = MASK_WIDTH * mask_y + mask_x;
	uint32_t mask_bit = mask_byte % 8;
	mask_byte >>= 3;
	return mask[mask_byte] & (0x80 >> mask_bit) != 0;
}

void draw_lin_rgb(uint64_t p, uint32_t its) {
	if(its < 5) {
		return;
	}
	uint32_t add_b = its;
	if(ITSTEPS_B < add_b)
		add_b = ITSTEPS_B;
	add_b -= 5;

	if ((pixels[p] += add_b) > brightest[0]) {
		brightest[0] = pixels[p];
	}
	if (its <= ITSTEPS_B) {
		return;
	}

	uint32_t add_g = its;
	if(ITSTEPS_G < add_g)
		add_g = ITSTEPS_G;
	if ((pixels[p + 1L] += add_g) > brightest[1]) {
		brightest[1] = pixels[p + 1L];
	}
	if (its <= ITSTEPS_G) {
		return;
	}

	uint32_t add_r = (uint32_t) pow(its, 1.05);
	if(ITSTEPS_R < add_r)
		add_r = ITSTEPS_R;
	if ((pixels[p + 2L] += add_r) > brightest[2]) {
		brightest[2] = pixels[p + 2L];
	}
}

void render_plane(double re) {
	double x, y, y2;
	uint64_t loc;
	uint32_t i = 0;
	for(double im = Y_MIN_CALC; im < 0; im += imstep) {
		if(!check_mask(re, im)) continue;

		double z[2] = {re, im};
		double c[2] = {re, im};
		double t = 0;

		for (i = 0; i < ITSTEPS_R && z[0] * z[0] + z[1] * z[1] < 16; i++) {
			if (i == ORDER) {
				x = z[0]; //(uint32_t) ((z[0] - X_MIN) / repixel);
				y = z[1]; //(uint32_t) ((z[1] - Y_MIN) / impixel);
				y2 = -z[1]; //(uint32_t) ((-z[1] - Y_MIN) / impixel);
				if(x < X_MIN_PLOT || x >= X_MAX_PLOT) {
					break;
				}
				if(y < Y_MIN_PLOT || y >= Y_MAX_PLOT) {
					if(y2 < Y_MIN_PLOT || y2 >= Y_MAX_PLOT) {
						break;
					}
				}
			}
			t = z[0] * z[0] - z[1] * z[1] + c[0];
			z[1] = 2 * z[0] * z[1] + c[1];
			z[0] = t;
		}
		if (i < ITSTEPS_R && i > ORDER) {
			uint32_t pixel_x = ((x - X_MIN_PLOT) / repixel);
			if(y >= Y_MIN_PLOT && y < Y_MAX_PLOT) {
				uint32_t pixel_y = ((y - Y_MIN_PLOT) / impixel);
				loc = WIDTH * pixel_y + pixel_x;
				draw_lin_rgb(3 * loc, i);
			}
			if(y2 >= Y_MIN_PLOT && y2 < Y_MAX_PLOT) {
				uint32_t pixel_y = ((y2 - Y_MIN_PLOT) / impixel);
				loc = WIDTH * pixel_y + pixel_x;
				draw_lin_rgb(3 * loc, i);
			}

		}
	}
}

void* render(void* thread_id) {
	uint32_t n = *((uint32_t*)thread_id);
	double chunk_offset = n * (X_MAX_CALC - X_MIN_CALC) / THREADS;
	double interlace_offset = n * restep;
	uint32_t q = 0;
	uint32_t total_steps = (X_MAX_CALC - X_MIN_CALC) / restep / THREADS;
	for(double re = X_MIN_CALC + interlace_offset + chunk_offset; re < X_MAX_CALC; re += THREADS * restep) {
		render_plane(re);
		if (n == 0 && ++q % 25 == 0) {
			printf("%d (%.2f%%) at %f\n", q, 100.0 * q / total_steps, re);
		}
	}
	for(double re = X_MIN_CALC + interlace_offset; re < X_MIN_CALC + chunk_offset; re += THREADS * restep) {
		render_plane(re);
		if (n == 0 && ++q % 25 == 0) {
			printf("%d (%.2f%%) at %f\n", q, 100.0 * q / total_steps, re);
		}
	}
	printf("Thread %d finished\n", n);
	return 0;
}

int main() {
	clock_t start, step, diff;
	start = clock();
	printf("Width %ld Height %ld\n", WIDTH, HEIGHT);
	printf("Loading mask..\n");
	FILE *fp;
	fp = fopen("mask.bin", "r");
	fread(mask, 1, MASK_WIDTH * MASK_HEIGHT / 8, fp);
	fclose(fp);

	step = clock();
	printf("%d Wiping buffer.. (%d)\n", (step - start), (step - start));
	uint64_t mem_size = 4L * 3L * WIDTH * HEIGHT;
	pixels = (uint32_t*) malloc(mem_size);
	if(pixels == NULL) {
		printf("failed to claim memory\n");
		return 0;
	} else {
		printf("Succ\n");
	}

	for(uint64_t i= 0; i < 3 * WIDTH * HEIGHT; i++)
	   pixels[i] = 0;

	diff = clock();
	printf("%d Baking Brot (%d)\n", (diff - start), (diff - step));
	step = diff;
	render_thread threads[THREADS];
	for(uint8_t i = 0; i < THREADS; i++) {
		threads[i].offset = i;

		// Multithreaded version
		pthread_create(&(threads[i].thread_id), NULL, render, &(threads[i].offset));

		// Singlethreaded version
		//render(&(threads[i].offset));
	}

	for(uint8_t i = 0; i < THREADS; i++) {
		pthread_join(threads[i].thread_id, NULL);
	}


	uint8_t* pixels_in_bytes = (uint8_t*) pixels;
	uint64_t gig_blocks;
	uint64_t gig_remainder;


	// Save the raw histogram after rendering. Creates a really big file, but is worth doing if your render takes very long and you want to tune the color mapping
	/*
	gig_blocks = (12L * WIDTH * HEIGHT) >> 30;
	gig_remainder = (12L * WIDTH * HEIGHT) & 0x3FFFFFFFL;

	fp = fopen("raw domp.bin", "wb");
	printf("Raw Dump file size: %luGB + %lu\n", gig_blocks, gig_remainder);


	for(uint64_t i = 0; i < gig_blocks; i++) {
		printf("Writing 1GB %lu/%lu %I64u..\n", i, gig_blocks, pixels_in_bytes + i * (1L << 30));
		fwrite((void*) (pixels_in_bytes + i * (1L << 30)), 1, 1 << 30, fp);
	}
	printf("Writing remainder..\n");
	fwrite((void*)(pixels_in_bytes + gig_blocks * (1L << 30)), 1, gig_remainder, fp);

	fclose(fp);
	*/





	diff = clock();
	printf("%d Transforming.. (%d)\n", (diff - start), (diff - step));
	step = diff;

	for(long p = 0; p < WIDTH * HEIGHT * 3; p += 3) {
		if ((pixels[p]) > brightest[0]) {
			brightest[0] = pixels[p];
		}
		if ((pixels[p + 1L]) > brightest[1]) {
			brightest[1] = pixels[p + 1L];
		}
		if ((pixels[p + 2L]) > brightest[2]) {
			brightest[2] = pixels[p + 2L];
		}
	}

	uint32_t total_brightest = brightest[2] > brightest[1] ? brightest[2] : brightest[1] > brightest[0] ? brightest[1] : brightest[0];
	double scale = SCALE_FACTOR / pow(total_brightest, GAMMA);


	uint32_t r, g, b;
	uint64_t p, q;
	for (uint64_t y = 0; y < HEIGHT; y++) {
		for (uint64_t x = 0; x < WIDTH; x++) {
			p = 3 * (WIDTH * y + x);
			// Blue pixels are sometimes decremented to increase contrast, we need to clamp negative pixels
			if ((b = (uint32_t) (pow(pixels[p], GAMMA) * scale)) > 255) {
				b = 255;
			}
			if ((g = (uint32_t) (pow(pixels[p + 1], GAMMA) * scale)) > 255) {
				g = 255;
			}
			if ((r = (uint32_t) (pow(pixels[p + 2], GAMMA) * scale)) > 255) {
				r = 255;
			}
			pixels[p / 3] = (r << 8) + (g << 16) + (b << 24);
		}
	}

	printf("Brightest: %d %d %d  -  %f..\n", brightest[0], brightest[1], brightest[2], scale);

	diff = clock();
	printf("%d Writing raw bitmap.. (%d)\n", (diff - start), (diff - step));
	step = diff;
	fp = fopen("domp.bin", "wb");

	gig_blocks = (4L * WIDTH * HEIGHT) >> 30;
	gig_remainder = (4L * WIDTH * HEIGHT) & 0x3FFFFFFFL;

	printf("Dump file size: %luGB + %lu\n", gig_blocks, gig_remainder);

	for(uint64_t i = 0; i < gig_blocks; i++) {
		printf("Writing 1GB %lu/%lu %I64u..\n", i, gig_blocks, pixels_in_bytes + i * (1L << 30));
		fwrite((void*) (pixels_in_bytes + i * (1L << 30)), 1, 1 << 30, fp);
	}
	printf("Writing remainder..\n");
	fwrite((void*)(pixels_in_bytes + gig_blocks * (1L << 30)), 1, gig_remainder, fp);

	fclose(fp);
	diff = clock();
	printf("%d Done (%d)\n", (diff - start), (diff - step));
	step = diff;
}

