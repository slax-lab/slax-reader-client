export type NativeInitialProps = {
  route?: string;
  language?: string;
  [key: string]: any;
};

let _initialProps: NativeInitialProps = {};

export function setInitialProps(props: NativeInitialProps) {
  _initialProps = props;
}

export function getInitialProps(): NativeInitialProps {
  return _initialProps;
}
