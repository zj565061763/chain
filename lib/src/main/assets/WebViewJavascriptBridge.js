;(function () {
  if (window.WebViewJavascriptBridge) {
    // 如果已经初始化过，直接return
    return
  }

  // JS本地注册的事件处理程序
  let messageHandlers = {}
  //  JS调用原生结果处理回调函数
  let responseCallbacks = {}

  let uniqueId = 1

  // 具体的JS向原生发送消息逻辑
  function _doSend(message, responseCallback) {
    if (responseCallback) {
      let callbackId = "cb_" + uniqueId++ + "_" + new Date().getTime()
      responseCallbacks[callbackId] = responseCallback
      message["callbackId"] = callbackId
    }
    let msg = JSON.stringify(message || {})
    // 原生注入的JSBridge对象
    if (window.WVJBInterface) {
      // JS向原生发送消息
      WVJBInterface.notice(msg)
    } else {
      // 出现错误，打印出日志
      _log( `WebViewJavascriptBridge: WARNING: 发送给原生方消息失败，没有注入WVJBInterface对象。${message}`)
    }
  }

  let bridge = {
    // 注册处理器
    registerHandler: function (handlerName, handler) {
      messageHandlers[handlerName] = handler
    },

    // JS调用原生函数
    callHandler: function (handlerName, data, responseCallback) {
      _log( `H5发送消息: ${handlerName} ${data}`)
//      if (arguments.length === 2 && typeof data === "function") {
//        responseCallback = data
//        data = null
//      }
      if(data) {
        data = jsBridgeParse(data)
      }
      _doSend(
        {
          handlerName: handlerName,
          data: data,
        },
        responseCallback
      )
    },

    // 原生调用JS
    _handleMessageFromJava: function (json) {
      _dispatchMessageFromJava(json)
    },

    // 检查原生是否提供指定的处理器支持
    hasNativeMethod: function (name, responseCallback) {
      this.callHandler("_hasNativeMethod", name, responseCallback)
    },
  }

  function jsBridgeParse(obj) {
    return JSON.parse(JSON.stringify(obj).replace('"', '"'))
  }

  // 检查JS是否提供指定的处理器支持
  bridge.registerHandler(
    "_hasJavascriptMethod",
    function (data, responseCallback) {
      responseCallback(!!messageHandlers[data])
    }
  )

  //   处理并且分发来自原生的消息，包括双向通讯的消息
  function _dispatchMessageFromJava(messageJson) {
    _log( `收到来自Native的消息 ${messageJson}`)
    if (!messageJson) {
      return
    }
    let message = JSON.parse(messageJson)
    let responseCallback = null
    if (message.responseId) {
      // 消息是JS调用原生之后的发来响应
      let responseCallback = responseCallbacks[message.responseId]
      if (!responseCallback) {
        return
      }
      responseCallback(message)
      delete responseCallbacks[message.responseId]
    } else {
      if (message.callbackId) {
        // 消息是原生调用JS发起的消息
        // 这里发送响应
        let callbackResponseId = message.callbackId
        responseCallback = function (responseData) {
          _doSend({
            handlerName: message.handlerName,
            responseId: callbackResponseId,
            responseData: responseData,
          })
        }
      }
      let handler = messageHandlers[message.handlerName]
      if (!handler) {
        console.log(
          "WebViewJavascriptBridge: WARNING: 没有消息处理器处理该消息",
          message
        )
      } else {
        handler(message.data, responseCallback)
      }
    }
  }

  function _log(message) {
      console.log("jssdk:", message)
  }


  // 因为通过原生注入此段初始化代码无法保证在 setupWebViewJavascriptBridge 之前执行，
  // 所以在这种情况下，WVJBCallbacks 中保存 setupWebViewJavascriptBridge传入的callback
  let callbacks = window.WVJBCallbacks
  delete window.WVJBCallbacks
  if (callbacks) {
    for (let cb of callbacks) {
      cb(bridge)
    }
  }
  window.WebViewJavascriptBridge = bridge

  console.log("注入函数执行完毕！")
})()
