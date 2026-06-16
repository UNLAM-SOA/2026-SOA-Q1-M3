module.exports = {
  flowFile: 'flows.json',
  credentialSecret: process.env.NODE_RED_CREDENTIAL_SECRET || 'grupoM3-soa-shared-secret',
  uiPort: process.env.PORT || 1880,
  diagnostics: {
    enabled: true,
    ui: true,
  },
  runtimeState: {
    enabled: false,
    ui: false,
  },
  logging: {
    console: {
      level: 'info',
      metrics: false,
      audit: false,
    },
  },
  exportGlobalContextKeys: false,
  externalModules: {
    autoInstall: false,
    autoInstallRetry: 30,
    palette: {
      allowInstall: true,
      allowUpload: true,
      allowList: [],
      denyList: [],
    },
    modules: {
      allowInstall: false,
      allowList: [],
      denyList: [],
    },
  },
  functionGlobalContext: {},
  editorTheme: {
    projects: {
      enabled: false,
    },
  },
};
