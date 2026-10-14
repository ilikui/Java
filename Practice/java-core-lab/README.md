# Java Core Lab

这是配套于 Java 学习知识库的可运行练习项目。示例以 **JDK 17 + Maven + JUnit 5** 为基线，重点训练集合 API、边界条件、测试设计与代码复盘。

## 环境要求

- JDK 17 或更高版本
- Maven 3.8+
- IDE 可选：IntelliJ IDEA / Eclipse / VS Code

确认环境：

```bash
java -version
mvn -version
```

## 运行

在本目录执行：

```bash
mvn test
mvn -q package
```

运行示例：

```bash
mvn -q exec:java -Dexec.mainClass="cn.ilikui.learning.CollectionLab"
```

如果未配置 Maven Exec 插件，可直接在 IDE 中运行 `CollectionLab.main`。

## 示例说明

- `countWords`：统计单词频次，练习 Map 的更新模式和输入规范化。
- `topK`：按频次降序、单词字典序升序返回前 K 项，练习排序规则和确定性输出。
- `safeAverage`：处理空集合与整数溢出风险，练习边界条件和数值类型选择。
- `CollectionLabTest`：JUnit 5 自动化测试，覆盖正常输入、空输入、大小写、非法参数和并列排序。
- `HashMapLab`：演示 `equals` / `hashCode` 契约与等价键覆盖行为。
- `ConcurrencyLab`：演示 `AtomicInteger` 原子更新。
- `HashMapLabTest` 与 `ConcurrencyLabTest`：覆盖键相等性与多线程计数。

## 练习任务

1. 为 `countWords` 增加对标点符号的处理，并定义连续标点的行为。
2. 为 `topK` 增加重复项与负数 K 的测试。
3. 实现一个按字段分组统计订单金额的方法。
4. 对每个新功能先写测试，再实现代码；不要只验证“正常路径”。
5. 将测试结果、失败原因和修复过程整理成一张复盘卡片。
6. 阅读 [HashMap 底层原理专题](../../Topics/13-HashMap底层原理与源码分析.md)，再修改 `EmployeeKey` 的相等性规则进行实验。

## 完成标准

- `mvn test` 全部通过。
- 公共方法有明确的参数约束和返回值约定。
- 边界条件有对应测试。
- 排序输出具有确定性，不依赖 HashMap 的迭代顺序。