// Dev-server proxy: every /api call goes to the backend (or the Prism mock).
// The target comes from API_TARGET; scripts/frontend.sh sets it (default: stable backend on 8090).
const target = process.env.API_TARGET || 'http://localhost:8090';

module.exports = {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'warn',
  },
};
