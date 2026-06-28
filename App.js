import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';

import ConnectionScreen from './src/screens/ConnectionScreen';
import KeyboardScreen from './src/screens/KeyboardScreen';
import AutoTypeScreen from './src/screens/AutoTypeScreen';

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <StatusBar style="light" />
        <Stack.Navigator
          initialRouteName="Connection"
          screenOptions={{ headerShown: false }}
        >
          <Stack.Screen name="Connection" component={ConnectionScreen} />
          <Stack.Screen name="Keyboard" component={KeyboardScreen} />
          <Stack.Screen name="AutoType" component={AutoTypeScreen} />
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
