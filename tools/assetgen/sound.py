"""Synthesized sound effects."""

from __future__ import annotations

import numpy as np
import soundfile as sf

from .output import Output

RATE = 44_100


def spray(seed: int = 112, duration: float = 0.75) -> np.ndarray:
    """Pressurized spray: band-passed noise with turbulence, a valve pop at the start and a soft release."""
    samples = int(RATE * duration)
    rng = np.random.default_rng(seed)
    t = np.arange(samples) / RATE

    # Hiss: white noise shaped by a log-normal band around 3.5 kHz.
    spectrum = np.fft.rfft(rng.standard_normal(samples))
    freqs = np.fft.rfftfreq(samples, 1 / RATE)
    band = np.exp(-(np.log(np.maximum(freqs, 1.0) / 3500.0) ** 2) / (2 * 0.75**2))
    hiss = np.fft.irfft(spectrum * band, n=samples)
    hiss /= np.max(np.abs(hiss))

    flutter = np.interp(t, np.linspace(0, duration, 12), rng.uniform(0.85, 1.0, 12))  # turbulence
    envelope = np.minimum(1.0, t / 0.025) * np.minimum(1.0, (duration - t) / 0.15)  # 25 ms attack, 150 ms release
    pop = 0.35 * np.sin(2 * np.pi * 90 * t) * np.exp(-t / 0.03)  # valve opening

    signal = hiss * flutter * envelope + pop
    return (0.89 * signal / np.max(np.abs(signal))).astype(np.float32)


def write(output: Output) -> None:
    path = output.file("sounds/feuerloescher/spray.ogg", "Feuerlöscher sprüht (synthetisch)")
    sf.write(path, spray(), RATE, format="OGG", subtype="VORBIS")
