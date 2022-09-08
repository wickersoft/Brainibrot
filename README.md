# Brainibrot
A collection of algorithms for generating Mandelbrot-derivatives

Java project is now considered legacy code. It contains experimental Mandelbrot-like rendering code.
The most interesting one has been ported to c, and its usage is summarized here:


# Installation
Make sure you have gcc with the pthread library available.
A quick start .bat file for windows is provided with re.bat.

# Usage
The default configuration is too large for a quick test. Setup is easy if you know where to look:
## Adjust parameters in the c file
The first 35 lines of the c file contain all the basic parameters of the render. Start with a height of a few thousand and a DPP value of 1-4. Adjust threads to your machine's capabilities and leave the rest alone.
## Run re.bat
re.bat will recompile the c file and execute it. After rendering is done, the c code produces a raw bitmap, which is converted to a PNG file by the java code.
## Large renders
The c code is configured such that it renders only the top half of the image to allow extremely high resolution renders to be done efficiently.
The image is symmetric across the x axis, so just use your image editing program when selecting a region.
If you configure your render to take multiple days, enable dumping of the raw histogram in line ~212. 
This allows you to change color mapping parameters without having to re-render the entire bitmap... theoretically.
The code to read this file back into memory has not been implemented.
It's easy though, you'll figure it out.

![I know](https://imgs.xkcd.com/comics/will_it_work.png)
