#### 生成 API 文档
首先需要安装 node，并安装 apidoc
``` bash
npm install apidoc -g
```
然后生成 api 文档
```
apidoc -i src/main/kotlin/datahub/api/controller -o apidoc
```

为了方面编写文档，可以在 IDEA 新建一个模板
```
File -> Settings -> Editor -> Live Templates -> Kotlin -> + Live Template
Abbreviation: apidoc
Descrpition: 生成接口注释
Template text: $DOC$
```
然后 edit variable，在 DOC 变量的 Expression 中输入下面的内容
```
groovyScript("
    def apiGroup = _3.replace('Controller', '');
    def result = '';
    def baseUrl = '';
    def url = '';
    def method = '';
    def currentLineNum = 0;
    def insertLineNum = _2.toInteger();
    def paramsClose = true;
    def params = '';
    new File(_1).eachLine {
        currentLineNum += 1;
        if (it.contains('RequestMapping(') && baseUrl == ''){
            baseUrl = it.substring(it.indexOf('/'), it.length() - 2);
        };
        if(currentLineNum >= insertLineNum && method == ''){
            if (it.contains('@GetMapping')){
                method = '{get} ';
                url = it;
            } else if(it.contains('@PostMapping')){
                method = '{post} ';
                url = it;
            } else if(it.contains('@PutMapping')){
                method = '{put} ';
                url = it;
            } else if(it.contains('@DeleteMapping')){
                method = '{delete} ';
                url = it;
            };
        };
        if(currentLineNum >= insertLineNum && method != ''){
            if(it.contains(' fun ') && params == ''){
                paramsClose = false;
            };
            if(!paramsClose){
                params += it.trim();
            };
            if(it.contains(': ResponseData')){
                paramsClose = true;
            };
        };
    };
    params = params.substring(params.indexOf('(') + 1, params.lastIndexOf(':') - 1);
    def paramsList = [];
    while(params != ''){
        def colonIndex = params.indexOf(':');
        def commaIndex = params.indexOf(',', colonIndex);
        if(commaIndex == -1){
            paramsList.add(params.substring(0));
            break;
        }else{
            paramsList.add(params.substring(0, commaIndex));
            params = params.substring(commaIndex + 1);
        };
    };
    def apiParams = '';
    paramsList.each {
        if(it.contains('@PathVariable')){
            return true;
        };
        def defaultValue = '';
        def dataType = it.substring(it.indexOf(':'));
        if(dataType.contains('?')){
            defaultValue = 'null';
        };
        if(dataType.contains('List')){
            dataType = '{Array} ';
        } else if(dataType.contains('Int')){
            dataType = '{Number} ';
        } else if(dataType.contains('String')){
            dataType = '{String} ';
        } else {
            dataType = '{Unknown} ';
        };
        def paramName = it.substring(0, it.indexOf(':'));
        if(paramName.contains('defaultValue')){
            defaultValue = paramName.substring(paramName.indexOf('defaultValue = ') + 16);
            defaultValue = defaultValue.substring(0, defaultValue.lastIndexOf(')') - 1);
        };
        paramName = paramName.substring(paramName.lastIndexOf(' ') + 1);
        if(defaultValue != ''){
            paramName = '[' + paramName + ' = ' + defaultValue + ']';
        };
        apiParams += '     * @apiParam ' + dataType + paramName + ' todo\\n';
    };
    url = url.contains('(') ? url.substring(url.indexOf('(') + 2, url.indexOf(')') - 1) : '';
    url = url.startsWith('{') ? '/' + url : url;
    return '/**\\n' + 
           '     * @api ' + method + baseUrl + url + ' todo\\n' +
           '     * @apiDescription todo\\n' + 
           '     * @apiGroup ' + apiGroup + '\\n' +
           '     * @apiVersion 0.1.0\\n' +
           '     * @apiHeader {String} token 用户授权 token\\n' + 
           apiParams + 
           '     * @apiSuccessExample 请求成功\\n' + 
           '     * {} \\n' +
           '     * @apiSuccessExample 请求失败\\n' + 
           '     * {} \\n' +
           '     */'
", filePath(), lineNumber(), kotlinClassName())
```
然后就可以在 Kotlin 代码中通过 apidoc 快捷方式插入注释