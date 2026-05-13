# 8 维度布局与标签定位

## 维度位置（正八边形顶点）

```
         传输速度(0)       仪式感(7)
           ↗                ↖
    编码效率(1)              电子感(5)
           ↑                  ↑
    传输稳定(2)              情感丰富度(6)
           ↖                ↗
    兼容性(3)       解析难度(4)
```

## 标签/数值布局规则

所有维度的标签（label）和数值（value `[n]`）沿法线方向偏移分离：

- **上/左/右维度** (0,1,2,5,6,7)：value 在 label 上方（屏幕空间）
- **下方维度** (3,4)：value 在 label 下方

## 方法调用链

```
draw_labels()
  └→ _label_value_positions(theta, r, half_gap, is_top)
       ├→ 计算 visual_theta = theta + 5π/8（补偿极坐标系旋转）
       ├→ s = sign(cos(visual_theta)) 决定屏幕空间方向
       ├→ 根据 is_top 确定 val_off / label_off
       └→ _shift_polar(theta, r, offset) × 2
            ├→ 极坐标 → 笛卡尔
            ├→ 沿法线方向偏移
            └→ 笛卡尔 → 极坐标
```

## 关键实现细节

### `visual_theta` 的作用

`set_theta_offset(5π/8)` 旋转了整个极坐标系，导致 raw angle 的 `cos θ` 符号与屏幕空间不一致。必须用 `visual_theta = theta + 5π/8` 计算偏移方向。

### `_shift_polar` 方法

```python
@staticmethod
def _shift_polar(angle, radius, offset, direction='perpendicular')
```

通用坐标转换工具，支持两种偏移方向：
- `'perpendicular'`（默认）：法线方向 `(-sin θ, cos θ)`，远离圆心
- `'radial'`：径向方向 `(cos θ, sin θ)`

### `_label_value_positions` 方法

```python
def _label_value_positions(self, theta, r, half_gap, is_top=True)
```

封装 label/value 的屏幕空间定位逻辑：
- `is_top=True`：value 在上，label 在下（用于 dims 0,1,2,5,6,7）
- `is_top=False`：value 在下，label 在上（用于 dims 3,4）

## 微调参数

| 参数 | 文件 | 说明 |
|------|------|------|
| `RADAR_LABEL_RADIUS_OFFSETS_*` | config_fonts.py | 各维度径向偏移（正=远离圆心）|
| `RADAR_LABEL_ANGLE_OFFSETS_*` | config_fonts.py | 各维度角度偏移（正=逆时针）|
| `HALF_GAP` | radar.py draw_labels() | label/value 间距（默认 0.6）|
| `SIZE_RADAR_LABEL` | config_fonts.py | 标签字号 |
