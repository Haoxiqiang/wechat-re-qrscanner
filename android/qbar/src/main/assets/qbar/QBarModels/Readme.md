qbar目录存放所有扫一扫相关的模型文件，包含扫码QBar引擎的检测模型、超分模型与角定位模型，存放在QBarModels目录下，此外还包含文字检测模型


### 目录结构
QBar引擎不同版本号的模型使用**版本号**为目录进行存放，可能包含检测模型、超分模型、角定位模型

### 目前的模型文件
qbar
  |-- QBarModels，存放当前在用的QBar模型
    |-- V1.3.0.26
        |-- qbar_detect.xnet, 检测模型xnet
    |-- V1.1.0.26
        |-- qbar_sr.xnet, 超分模型xnet
        |-- qbar_seg.xnet, 角定位模型xnet
  |-- net_fc.bin, net_fc.param, 文字检测模型(ncnn)
  |-- net_fc.param