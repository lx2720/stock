var pageConfiguration = {
  needAuth: false
};

$(function() {
  if (pageConfiguration.needAuth) {
    if (!isLocalAuth()) {
      LocationUtil.goto('/login.html');
      return;
    }
  }

  renderHead();
  renderFoot();

  if (onload) {
    onload();
  }

  function renderHead() {
    var content = '<div class="innerBox"><ul class="top-head">';
    if (!isLocalAuth()) {
      content += '<li><a href="/user/login.html">登录</a></li>';
    } else {
      content += '<li><a href="/user/profile.html">个人中心</a></li>';
      content += '<li><a href="/user/logout.html">注销</a></li>';
    }
    content += '</ul></div>';
    $('#head').html(content);
  }

  function renderFoot() {
    var content = '<div class="innerBox">';
    content += '</div>';
    $('#foot').html(content);
  }

  function isLocalAuth() {
    /*var token = getHeaders()['auth-token'];
    return token && token != null && token.length > 10;*/
    return true;
  }

});

function renderMenu(arr, selector, current) {
  var content = '';
  $.each(arr, function(index, item) {
    if (current === item.id) {
      content += '<li class="current"><a href="javascript:void(0);">' + item.title +'</a></li>';
    } else {
      content += '<li ><a href="' + item.url + '">' + item.title +'</a></li>';
    }
  });

  $(selector).html(content);
}

function getRequestHeaders() {
  return {
    'auth-token': StorageUtil.get(GlobalConsts.authTokenKey)
  }
}
