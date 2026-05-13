"""test_radar.py — _shift_polar 基础测试"""

import numpy as np
import pytest
from radar import RadarChart


shift = RadarChart._shift_polar


def _to_cartesian(radius, angle):
    """极坐标 → 笛卡尔，参数顺序与 _shift_polar 返回值一致。"""
    return radius * np.cos(angle), radius * np.sin(angle)


def _normalize(angle):
    """将角度归一化到 [0, 2π)。"""
    return angle % (2 * np.pi)


class TestShiftPolarZeroOffset:
    """零偏移应返回原始坐标。"""

    def test_zero_offset_preserves_r(self):
        for angle in [0, np.pi / 4, np.pi, 3 * np.pi / 2]:
            r, _ = shift(angle, 10.0, 0.0)
            assert r == pytest.approx(10.0)

    def test_zero_offset_preserves_angle(self):
        for angle in [0, np.pi / 4, np.pi, 3 * np.pi / 2]:
            _, theta = shift(angle, 10.0, 0.0)
            assert _normalize(theta) == pytest.approx(_normalize(angle))


class TestShiftPolarRadial:
    """径向偏移：沿 (cos θ, sin θ) 方向移动，r 变化、θ 不变。"""

    def test_radial_outward(self):
        for angle in [0, np.pi / 2, np.pi, 3 * np.pi / 2]:
            r, theta = shift(angle, 10.0, 1.0, direction='radial')
            assert r == pytest.approx(11.0)
            assert _normalize(theta) == pytest.approx(_normalize(angle))

    def test_radial_inward(self):
        r, _ = shift(0, 10.0, -2.0, direction='radial')
        assert r == pytest.approx(8.0)


class TestShiftPolarPerpendicular:
    """法线偏移：沿 (-sin θ, cos θ) 方向移动。"""

    def test_angle_0_moves_up(self):
        """θ=0：法线 (0,1)，正偏移 → y 增大。"""
        _, cy_before = _to_cartesian(10.0, 0)
        _, cy_after = _to_cartesian(*shift(0, 10.0, 1.0))
        assert cy_after > cy_before

    def test_angle_90_moves_left(self):
        """θ=π/2：法线 (-1,0)，正偏移 → x 减小。"""
        cx_before, _ = _to_cartesian(10.0, np.pi / 2)
        cx_after, _ = _to_cartesian(*shift(np.pi / 2, 10.0, 1.0))
        assert cx_after < cx_before

    def test_angle_180_moves_down(self):
        """θ=π：法线 (0,-1)，正偏移 → y 减小。"""
        _, cy_before = _to_cartesian(10.0, np.pi)
        _, cy_after = _to_cartesian(*shift(np.pi, 10.0, 1.0))
        assert cy_after < cy_before

    def test_angle_270_moves_right(self):
        """θ=3π/2：法线 (1,0)，正偏移 → x 增大。"""
        cx_before, _ = _to_cartesian(10.0, 3 * np.pi / 2)
        cx_after, _ = _to_cartesian(*shift(3 * np.pi / 2, 10.0, 1.0))
        assert cx_after > cx_before

    def test_negative_offset_reverses(self):
        """负偏移方向相反。"""
        _, cy_pos = _to_cartesian(*shift(0, 10.0, 1.0))
        _, cy_neg = _to_cartesian(*shift(0, 10.0, -1.0))
        assert cy_pos > cy_neg


class TestShiftPolarDiagonal:
    """对角线角度验证笛卡尔分量。"""

    def test_45_deg_components(self):
        """θ=π/4：法线 (-√2/2, √2/2)，正偏移 → x 减 y 增。"""
        cx_before, cy_before = _to_cartesian(10.0, np.pi / 4)
        cx_after, cy_after = _to_cartesian(*shift(np.pi / 4, 10.0, 1.0))
        assert cx_after < cx_before
        assert cy_after > cy_before

    def test_135_deg_components(self):
        """θ=3π/4：法线 (-√2/2, -√2/2)，正偏移 → x 减 y 减。"""
        cx_before, cy_before = _to_cartesian(10.0, 3 * np.pi / 4)
        cx_after, cy_after = _to_cartesian(*shift(3 * np.pi / 4, 10.0, 1.0))
        assert cx_after < cx_before
        assert cy_after < cy_before


if __name__ == '__main__':
    pytest.main([__file__, '-v'])
