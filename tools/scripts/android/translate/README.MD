先run.py生成
## ai_translation_reviews
这个是用来输出英文原句，以及翻译后的语言进行对比的md
也有英语单独的md,用于检验英文原句润色如何
## mixed_language_reports
用于检查特定语言中，是否有混杂其他的语言，比如中文混杂了英文
但是会忽略LED 这种单词，中文/俄语之类允许LED这种单词



## apply_translation_replacements
让agent阅读ai_translation_reviews，给出修改建议的json，放入tools/scripts/android/translate/replacements.json，来进行替换
替换前和替换后，都会检查xml的格式，都正确才允许改动
python -u "c:\code\WaveBits\tools\scripts\android\translate\run.py" replace


