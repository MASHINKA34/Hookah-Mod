$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $root "src/main/resources/assets/hookahmod/textures/block/hookah.png"
$outputDir = Join-Path $root "src/main/resources/assets/hookahmod/textures/block"

$tiers = @{
    leather = @{
        Shadow = [System.Drawing.Color]::FromArgb(0x3a, 0x20, 0x12)
        Mid = [System.Drawing.Color]::FromArgb(0x7a, 0x45, 0x24)
        High = [System.Drawing.Color]::FromArgb(0xb8, 0x75, 0x3a)
        Top = [System.Drawing.Color]::FromArgb(0xe1, 0xad, 0x72)
    }
    gold = @{
        Shadow = [System.Drawing.Color]::FromArgb(0x5c, 0x3d, 0x00)
        Mid = [System.Drawing.Color]::FromArgb(0xb8, 0x86, 0x0b)
        High = [System.Drawing.Color]::FromArgb(0xff, 0xd5, 0x4a)
        Top = [System.Drawing.Color]::FromArgb(0xff, 0xf3, 0xb0)
    }
    iron = @{
        Shadow = [System.Drawing.Color]::FromArgb(0x3a, 0x3a, 0x3a)
        Mid = [System.Drawing.Color]::FromArgb(0x6b, 0x6b, 0x6b)
        High = [System.Drawing.Color]::FromArgb(0xc8, 0xc8, 0xc8)
        Top = [System.Drawing.Color]::FromArgb(0xee, 0xee, 0xee)
    }
    diamond = @{
        Shadow = [System.Drawing.Color]::FromArgb(0x1f, 0x6e, 0x6e)
        Mid = [System.Drawing.Color]::FromArgb(0x4a, 0xd6, 0xd6)
        High = [System.Drawing.Color]::FromArgb(0x9f, 0xf5, 0xf5)
        Top = [System.Drawing.Color]::FromArgb(0xdd, 0xff, 0xff)
    }
    netherite = @{
        Shadow = [System.Drawing.Color]::FromArgb(0x2a, 0x24, 0x28)
        Mid = [System.Drawing.Color]::FromArgb(0x3b, 0x35, 0x39)
        High = [System.Drawing.Color]::FromArgb(0x5a, 0x52, 0x58)
        Top = [System.Drawing.Color]::FromArgb(0x9a, 0x90, 0x9a)
    }
}

$regions = @(
    @{ X1 = 0.0; Y1 = 4.0; X2 = 6.0; Y2 = 8.0 },
    @{ X1 = 2.0; Y1 = 1.25; X2 = 4.25; Y2 = 3.0 },
    @{ X1 = 10.0; Y1 = 4.0; X2 = 12.0; Y2 = 4.75 },
    @{ X1 = 6.0; Y1 = 4.0; X2 = 8.0; Y2 = 5.5 },
    @{ X1 = 6.0; Y1 = 7.0; X2 = 8.0; Y2 = 10.0 },
    @{ X1 = 8.0; Y1 = 8.5; X2 = 10.0; Y2 = 10.0 }
)

function Test-Region($x, $y, $width, $height) {
    $u = ($x + 0.5) * 16.0 / $width
    $v = ($y + 0.5) * 16.0 / $height
    foreach ($region in $regions) {
        if ($u -ge $region.X1 -and $u -lt $region.X2 -and $v -ge $region.Y1 -and $v -lt $region.Y2) {
            return $true
        }
    }
    return $false
}

function Get-RemappedColor($color, $palette) {
    if ($color.A -eq 0) {
        return $color
    }
    $light = (0.299 * $color.R + 0.587 * $color.G + 0.114 * $color.B) / 255.0
    if ($light -lt 0.24) {
        $target = $palette.Shadow
    } elseif ($light -lt 0.52) {
        $target = $palette.Mid
    } elseif ($light -lt 0.78) {
        $target = $palette.High
    } else {
        $target = $palette.Top
    }
    $alpha = if ($color.A -lt 255) { [Math]::Max($color.A, 210) } else { 255 }
    return [System.Drawing.Color]::FromArgb($alpha, $target.R, $target.G, $target.B)
}

$source = [System.Drawing.Bitmap]::new($sourcePath)
try {
    foreach ($tier in $tiers.Keys) {
        $copy = [System.Drawing.Bitmap]::new($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            for ($y = 0; $y -lt $source.Height; $y++) {
                for ($x = 0; $x -lt $source.Width; $x++) {
                    $color = $source.GetPixel($x, $y)
                    if (Test-Region $x $y $source.Width $source.Height) {
                        $color = Get-RemappedColor $color $tiers[$tier]
                    }
                    $copy.SetPixel($x, $y, $color)
                }
            }
            $targetPath = Join-Path $outputDir "hookah_$tier.png"
            $copy.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $copy.Dispose()
        }
    }
} finally {
    $source.Dispose()
}
