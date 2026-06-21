Component({
  data: {
    value: 'home',
    list: [
      { icon: 'home', value: 'home', label: '首页' },
      { icon: 'app', value: 'business', label: '业务' },
      { icon: 'user', value: 'my', label: '我的' },
    ],
  },
  lifetimes: {
    ready() {
      const pages = getCurrentPages();
      const curPage = pages[pages.length - 1];
      if (curPage?.route) {
        const nameRe = /pages\/(\w+)\/index/.exec(curPage.route);
        if (nameRe?.[1]) {
          this.setData({ value: nameRe[1] });
        }
      }
    },
  },
  methods: {
    handleChange(e) {
      const { value } = e.detail;
      if (!wx.getStorageSync('access_token')) {
        wx.reLaunch({ url: '/pages/login/login' });
        return;
      }
      wx.switchTab({ url: `/pages/${value}/index` });
    },
  },
});
