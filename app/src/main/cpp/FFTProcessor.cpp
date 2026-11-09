#include <vector>
#include <complex>
#include <cmath>

// Starter radix-2 FFT module. Production integration should add input windowing,
// PCM normalization, frequency-bin mapping and real-time allocation management.
namespace audiovis {
    void fft(std::vector<std::complex<float>>& a) {
        const size_t n = a.size();
        if (n <= 1 || (n & (n - 1)) != 0) return;
        for (size_t i = 1, j = 0; i < n; ++i) {
            size_t bit = n >> 1;
            for (; j & bit; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) std::swap(a[i], a[j]);
        }
        for (size_t len = 2; len <= n; len <<= 1) {
            const float angle = -2.0f * 3.14159265358979323846f / float(len);
            const std::complex<float> wlen(std::cos(angle), std::sin(angle));
            for (size_t i = 0; i < n; i += len) {
                std::complex<float> w(1.0f, 0.0f);
                for (size_t j = 0; j < len / 2; ++j) {
                    auto u = a[i + j];
                    auto v = a[i + j + len / 2] * w;
                    a[i + j] = u + v;
                    a[i + j + len / 2] = u - v;
                    w *= wlen;
                }
            }
        }
    }
}
