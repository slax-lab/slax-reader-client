import {ImageSourcePropType, Platform} from "react-native";

const imageAssets = {
    ic_nav_back: require('../images/ic_nav_back.png'),
    ic_agree_enable: require('../images/ic_agree_enable.png'),
    ic_agree_disable: require('../images/ic_agree_disable.png'),
};

type ImageName = keyof typeof imageAssets;

export const getImageSource = (name: ImageName): ImageSourcePropType => {
    if (__DEV__ || Platform.OS === 'android') {
        return imageAssets[name];
    }
    return { uri: name };
};
