/**
 * Icon Generator for Google Play Store — Voice Recorder
 *
 * Generates all required icon sizes and graphics from SVG sources.
 * Run: npm run generate-icons
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Check if dependencies are installed
try {
    require('sharp');
    require('@resvg/resvg-js');
} catch (err) {
    console.log('📦 Installing required dependencies...\n');
    execSync('npm install', { stdio: 'inherit', cwd: path.join(__dirname, '..') });
    console.log('\n✅ Dependencies installed!\n');
}

const sharp = require('sharp');
const { Resvg } = require('@resvg/resvg-js');

// Paths
const ASSETS_DIR = path.join(__dirname, '..', 'assets', 'images');
const OUTPUT_DIR = path.join(__dirname, '..', 'play-store-assets');
const SVG_LOGO = path.join(ASSETS_DIR, 'app-logo.svg');            // full-bleed rounded square icon
const SVG_ICON_CIRCLE = path.join(ASSETS_DIR, 'app-icon-modern.svg');     // circle icon (web/launcher)
const SVG_ICON_FOREGROUND = path.join(ASSETS_DIR, 'app-icon-foreground.svg'); // mic glyph, transparent
const BRAND_COLOR = '#37474F';

// Ensure output directory exists
if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

console.log('🎨 Voice Recorder — Play Store Asset Generator\n');
console.log('================================================\n');

/**
 * Convert SVG to PNG using resvg-js
 */
async function convertSvgToPng(svgPath, outputPath, width, height = null) {
    const actualHeight = height || width;

    try {
        const svgBuffer = fs.readFileSync(svgPath);
        const resvg = new Resvg(svgBuffer, {
            fitTo: {
                mode: 'width',
                value: width
            }
        });

        const pngData = resvg.render();
        const pngBuffer = pngData.asPng();

        if (actualHeight !== width) {
            await sharp(pngBuffer)
                .resize(width, actualHeight)
                .toFile(outputPath);
        } else {
            fs.writeFileSync(outputPath, pngBuffer);
        }

        return true;
    } catch (err) {
        console.error(`   ❌ Error converting ${path.basename(svgPath)}:`, err.message);
        return false;
    }
}

/**
 * Generate the Play Store app icon (512x512, full bleed, no transparency)
 */
async function generateAppIcon() {
    console.log('📱 Generating App Icon...');

    const outputPath = path.join(OUTPUT_DIR, 'icon-512.png');
    const success = await convertSvgToPng(SVG_LOGO, outputPath, 512);

    if (success) {
        console.log('   ✅ Created: icon-512.png (512x512, full bleed)');
    }

    return success;
}

/**
 * Generate adaptive icon foreground/background/monochrome layers (108dp safe-zone style)
 */
async function generateAdaptiveIcons() {
    console.log('\n📐 Generating Adaptive Icons...');

    // Foreground: mic glyph on transparent
    const foregroundPath = path.join(OUTPUT_DIR, 'adaptive-icon-foreground.png');
    let success = await convertSvgToPng(SVG_ICON_FOREGROUND, foregroundPath, 512);
    if (success) {
        console.log('   ✅ adaptive-icon-foreground.png (512x512, transparent)');
    }

    // Background: solid brand color
    const backgroundPath = path.join(OUTPUT_DIR, 'adaptive-icon-background.png');
    await sharp({
        create: {
            width: 512,
            height: 512,
            channels: 4,
            background: BRAND_COLOR
        }
    })
        .png()
        .toFile(backgroundPath);
    console.log('   ✅ adaptive-icon-background.png (512x512 solid)');

    // Monochrome: white glyph on transparent (same as foreground for this icon)
    const monochromePath = path.join(OUTPUT_DIR, 'adaptive-icon-monochrome.png');
    fs.copyFileSync(foregroundPath, monochromePath);
    console.log('   ✅ adaptive-icon-monochrome.png (512x512, transparent)');

    // Combined preview: background + foreground composited
    const combinedPath = path.join(OUTPUT_DIR, 'adaptive-icon.png');
    await sharp(backgroundPath)
        .composite([{ input: foregroundPath }])
        .png()
        .toFile(combinedPath);
    console.log('   ✅ adaptive-icon.png (512x512 combined preview)');
}

/**
 * Generate launcher icons (full-bleed rounded icon at common densities)
 */
async function generateLauncherIcons() {
    console.log('\n🚀 Generating Launcher Icons...');

    const sizes = [48, 72, 96, 144, 192, 512];

    for (const size of sizes) {
        const outputPath = path.join(OUTPUT_DIR, `launcher-icon-${size}.png`);
        const success = await convertSvgToPng(SVG_LOGO, outputPath, size);
        if (success) {
            console.log(`   ✅ ${size}x${size}`);
        }
    }
}

/**
 * Generate web favicons (circle icon)
 */
async function generateWebAssets() {
    console.log('\n🌐 Generating Web Assets...');

    const faviconSizes = [16, 32, 48];
    for (const size of faviconSizes) {
        const outputPath = path.join(OUTPUT_DIR, `favicon-${size}.png`);
        const success = await convertSvgToPng(SVG_ICON_CIRCLE, outputPath, size);
        if (success) {
            console.log(`   ✅ Favicon ${size}x${size}`);
        }
    }
}

/**
 * Generate feature graphic (1024x500, brand color background + centered icon)
 */
async function generateFeatureGraphic() {
    console.log('\n🖼️  Generating Feature Graphic...');

    const featurePath = path.join(OUTPUT_DIR, 'feature-graphic.png');
    const tempIcon = path.join(OUTPUT_DIR, 'temp-feature-icon.png');

    await convertSvgToPng(SVG_ICON_CIRCLE, tempIcon, 360);

    await sharp({
        create: {
            width: 1024,
            height: 500,
            channels: 4,
            background: BRAND_COLOR
        }
    })
        .composite([{ input: tempIcon, gravity: 'center' }])
        .png()
        .toFile(featurePath);

    fs.unlinkSync(tempIcon);

    console.log('   ✅ Feature Graphic (1024x500)');
    console.log('   ℹ️  Note: Add app name text manually using an image editor if desired');
}

/**
 * Main execution
 */
async function main() {
    try {
        for (const svg of [SVG_LOGO, SVG_ICON_CIRCLE, SVG_ICON_FOREGROUND]) {
            if (!fs.existsSync(svg)) {
                console.error('❌ SVG source not found at:', svg);
                process.exit(1);
            }
        }

        await generateAppIcon();
        await generateAdaptiveIcons();
        await generateLauncherIcons();
        await generateWebAssets();
        await generateFeatureGraphic();

        console.log('\n✨ All assets generated successfully!');
        console.log(`📁 Output folder: ${OUTPUT_DIR}`);
        console.log('\n📋 Next Steps:');
        console.log('   1. Review generated assets in play-store-assets/');
        console.log('   2. Optionally edit feature-graphic.png to add branding/text');
        console.log('   3. Take real phone screenshots for the listing');

    } catch (error) {
        console.error('\n❌ Error:', error.message);
        process.exit(1);
    }
}

main();
